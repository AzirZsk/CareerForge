package com.landit.interview.handler;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.landit.common.enums.InterviewSource;
import com.landit.common.exception.BusinessException;
import com.landit.common.response.PageResponse;
import com.landit.company.entity.Company;
import com.landit.company.service.CompanyService;
import com.landit.interview.dto.interviewcenter.*;
import com.landit.interview.entity.Interview;
import com.landit.interview.entity.InterviewPreparation;
import com.landit.interview.entity.InterviewReviewNote;
import com.landit.interview.graph.preparation.InterviewPreparationGraphService;
import com.landit.interview.graph.review.ReviewAnalysisGraphService;
import com.landit.interview.service.InterviewCenterService;
import com.landit.interview.service.InterviewPreparationService;
import com.landit.interview.service.InterviewReviewNoteService;
import com.landit.jobposition.entity.JobPosition;
import com.landit.jobposition.service.JobPositionService;
import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.entity.Resume;
import com.landit.resume.service.ResumeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

// 使用完整限定名避免常量冲突
import com.landit.interview.graph.preparation.InterviewPreparationGraphConstants;
import com.landit.interview.graph.review.ReviewAnalysisGraphConstants;

/**
 * 面试中心业务编排处理器
 * 负责真实面试的业务逻辑编排
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewCenterHandler {

    private static final long SSE_TIMEOUT = 300_000L; // 5分钟超时

    private final InterviewCenterService interviewCenterService;
    private final InterviewPreparationService preparationService;
    private final InterviewReviewNoteService reviewNoteService;
    private final JobPositionService jobPositionService;
    private final CompanyService companyService;
    private final InterviewPreparationGraphService preparationGraphService;
    private final ReviewAnalysisGraphService reviewGraphService;
    private final ResumeService resumeService;
    private final ObjectMapper objectMapper;

    /**
     * 创建真实面试
     */
    @Transactional(rollbackFor = Exception.class)
    public InterviewDetailVO createInterview(CreateInterviewRequest request) {
        log.info("创建真实面试: jobPositionId={}, company={}, position={}",
                request.getJobPositionId(), request.getCompanyName(), request.getPosition());
        // 业务校验：必须关联简历
        if (request.getResumeId() == null || request.getResumeId().isBlank()) {
            throw new BusinessException("创建面试必须关联简历");
        }
        // 业务校验：如果没有关联职位，则必须提供公司名和职位名
        String jobPositionId = request.getJobPositionId();
        boolean hasJobPositionId = jobPositionId != null && !jobPositionId.isBlank();
        if (!hasJobPositionId) {
            // 新建职位模式：必须提供公司名和职位名
            if (request.getCompanyName() == null || request.getCompanyName().isBlank()) {
                throw new BusinessException("公司名称不能为空");
            }
            if (request.getPosition() == null || request.getPosition().isBlank()) {
                throw new BusinessException("职位名称不能为空");
            }
            // 新建职位模式：查找或创建 JobPosition
            jobPositionId = findOrCreateJobPosition(request.getCompanyName(), request.getPosition(), request.getJdContent());
        }
        Interview interview = new Interview();
        interview.setSource(InterviewSource.REAL.getCode());
        interview.setStatus("preparing");
        // company 和 position 通过 jobPositionId 关联查询，不再冗余存储
        interview.setDate(request.getInterviewDate());
        interview.setJdContent(request.getJdContent());
        interview.setNotes(request.getNotes());
        interview.setCompanyResearch("{}");
        interview.setJdAnalysis("{}");
        interview.setJobPositionId(jobPositionId);
        interview.setResumeId(request.getResumeId());
        interview.setRoundType(request.getRoundType());
        interview.setRoundName(request.getRoundName());
        interview.setInterviewType(request.getInterviewType());
        interview.setLocation(request.getLocation());
        interview.setOnlineLink(request.getOnlineLink());
        interview.setMeetingPassword(request.getMeetingPassword());
        interviewCenterService.save(interview);
        log.info("创建真实面试成功: id={}, jobPositionId={}, roundType={}",
                interview.getId(), interview.getJobPositionId(), request.getRoundType());
        return getInterviewDetail(interview.getId());
    }

    /**
     * 获取面试列表
     */
    public PageResponse<InterviewListItemVO> getInterviewList(String type, String status, Integer page, Integer size) {
        LambdaQueryWrapper<Interview> wrapper = new LambdaQueryWrapper<>();
        if (!"all".equals(type)) {
            wrapper.eq(Interview::getSource, type);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Interview::getStatus, status);
        }
        wrapper.orderByDesc(Interview::getDate);
        Page<Interview> pageParam = new Page<>(page, size);
        Page<Interview> result = interviewCenterService.page(pageParam, wrapper);
        List<InterviewListItemVO> voList = result.getRecords().stream()
                .map(this::convertToListItemVO)
                .collect(Collectors.toList());
        return PageResponse.of(voList, result.getTotal(), page, size);
    }

    /**
     * 获取面试详情
     */
    public InterviewDetailVO getInterviewDetail(String id) {
        Interview interview = interviewCenterService.getById(id);
        if (interview == null) {
            throw new BusinessException("面试不存在: " + id);
        }
        List<InterviewPreparation> preparations = preparationService.getByInterviewId(id);
        InterviewReviewNote reviewNote = reviewNoteService.getManualNoteByInterviewId(id);
        return convertToDetailVO(interview, preparations, reviewNote);
    }

    /**
     * 更新面试基本信息
     */
    @Transactional(rollbackFor = Exception.class)
    public InterviewDetailVO updateInterview(String id, UpdateInterviewRequest request) {
        log.info("更新面试: id={}", id);
        Interview interview = interviewCenterService.getById(id);
        if (interview == null) {
            throw new BusinessException("面试不存在: " + id);
        }
        // company 和 position 通过 jobPositionId 关联查询，不再冗余存储
        if (request.getInterviewDate() != null) {
            interview.setDate(request.getInterviewDate());
        }
        if (request.getJdContent() != null) {
            interview.setJdContent(request.getJdContent());
        }
        if (request.getNotes() != null) {
            interview.setNotes(request.getNotes());
        }
        if (request.getRoundType() != null && !request.getRoundType().isBlank()) {
            interview.setRoundType(request.getRoundType());
        }
        if (request.getRoundName() != null) {
            interview.setRoundName(request.getRoundName());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            interview.setStatus(request.getStatus());
        }
        if (request.getOverallResult() != null) {
            interview.setOverallResult(request.getOverallResult());
        }
        if (request.getInterviewType() != null) {
            interview.setInterviewType(request.getInterviewType());
        }
        if (request.getLocation() != null) {
            interview.setLocation(request.getLocation());
        }
        if (request.getOnlineLink() != null) {
            interview.setOnlineLink(request.getOnlineLink());
        }
        if (request.getMeetingPassword() != null) {
            interview.setMeetingPassword(request.getMeetingPassword());
        }
        if (request.getResumeId() != null) {
            interview.setResumeId(request.getResumeId());
        }
        interviewCenterService.updateById(interview);
        log.info("更新面试成功: id={}", id);
        return getInterviewDetail(id);
    }

    /**
     * 删除面试
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteInterview(String id) {
        log.info("删除面试: id={}", id);
        Interview interview = interviewCenterService.getById(id);
        if (interview == null) {
            throw new BusinessException("面试不存在: " + id);
        }
        reviewNoteService.deleteByInterviewId(id);
        preparationService.deleteByInterviewId(id);
        interviewCenterService.removeById(id);
        log.info("删除面试成功: id={}", id);
    }

    // ===== 工作流 SSE 方法 =====

    /**
     * 流式执行面试准备工作流
     *
     * @param id       面试ID
     * @param response HTTP响应
     * @return SSE事件发射器
     */
    public SseEmitter streamPreparation(String id, HttpServletResponse response) {
        log.info("开始SSE流式面试准备: interviewId={}", id);
        InterviewDetailVO interview = getInterviewDetail(id);
        configureSseResponse(response);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        String threadId = UUID.randomUUID().toString();
        // 构建初始状态
        Map<String, Object> initialState = new HashMap<>();
        initialState.put(InterviewPreparationGraphConstants.STATE_INTERVIEW_ID, id);
        initialState.put(InterviewPreparationGraphConstants.STATE_COMPANY_NAME, interview.getCompanyName());
        initialState.put(InterviewPreparationGraphConstants.STATE_POSITION_TITLE, interview.getPosition());
        initialState.put(InterviewPreparationGraphConstants.STATE_JD_CONTENT, interview.getJdContent());
        // 注入简历内容（如果有关联简历）
        if (interview.getResumeId() != null && !interview.getResumeId().isBlank()) {
            String resumeContent = buildResumeContext(interview.getResumeId());
            initialState.put(InterviewPreparationGraphConstants.STATE_RESUME_CONTENT, resumeContent);
        }
        // 查询同一职位上一轮面试的复盘笔记
        String previousReviewNotesJson = getPreviousReviewNotesJson(interview.getJobPositionId(), id);
        if (previousReviewNotesJson != null) {
            initialState.put(InterviewPreparationGraphConstants.STATE_PREVIOUS_REVIEW_NOTES, previousReviewNotesJson);
        }
        initialState.put(InterviewPreparationGraphConstants.STATE_MESSAGES, new ArrayList<String>());
        // 发送开始事件
        sendSseEvent(emitter, GraphProgressEvent.startPreparation(id, threadId,
                interview.getCompanyName(), interview.getPosition()));
        // 订阅工作流（在独立线程池执行，避免阻塞调用线程）
        preparationGraphService.streamPreparation(initialState, threadId)
                .filter(output -> !isInterruptionOutput(output))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        output -> {
                            Map<String, Object> data = output.state().data();
                            @SuppressWarnings("unchecked")
                            Map<String, Object> nodeOutput = (Map<String, Object>) data.get(InterviewPreparationGraphConstants.STATE_NODE_OUTPUT);
                            GraphProgressEvent event = GraphProgressEvent.fromNodeOutput(output.node(), threadId, nodeOutput);
                            sendSseEvent(emitter, event);
                        },
                        error -> {
                            log.error("[SSE] 面试准备工作流异常", error);
                            sendSseEvent(emitter, GraphProgressEvent.error(error.getMessage(), threadId));
                            emitter.completeWithError(error);
                        },
                        () -> {
                            log.info("[SSE] 面试准备工作流完成: threadId={}", threadId);
                            sendSseEvent(emitter, GraphProgressEvent.completePreparation(threadId, Collections.emptyList()));
                            emitter.complete();
                        }
                );
        return emitter;
    }

    /**
     * 流式执行复盘分析工作流
     *
     * @param id                面试ID
     * @param sessionTranscript 面试过程文本（用户输入）
     * @param response          HTTP响应
     * @return SSE事件发射器
     */
    public SseEmitter streamReviewAnalysis(String id, String sessionTranscript, HttpServletResponse response) {
        log.info("开始SSE流式复盘分析: interviewId={}", id);
        InterviewDetailVO interview = getInterviewDetail(id);
        configureSseResponse(response);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        String threadId = UUID.randomUUID().toString();
        // 构建初始状态
        Map<String, Object> initialState = new HashMap<>();
        initialState.put(ReviewAnalysisGraphConstants.STATE_INTERVIEW_ID, id);
        initialState.put(ReviewAnalysisGraphConstants.STATE_SESSION_TRANSCRIPT, sessionTranscript);
        initialState.put(ReviewAnalysisGraphConstants.STATE_MESSAGES, new ArrayList<String>());
        // 发送开始事件
        sendSseEvent(emitter, GraphProgressEvent.startReviewAnalysis(id, threadId));
        // 订阅工作流（在独立线程池执行，避免阻塞调用线程）
        reviewGraphService.streamAnalysis(initialState, threadId)
                .filter(output -> !isInterruptionOutput(output))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        output -> {
                            Map<String, Object> data = output.state().data();
                            @SuppressWarnings("unchecked")
                            Map<String, Object> nodeOutput = (Map<String, Object>) data.get(ReviewAnalysisGraphConstants.STATE_NODE_OUTPUT);
                            GraphProgressEvent event = GraphProgressEvent.fromNodeOutput(output.node(), threadId, nodeOutput);
                            sendSseEvent(emitter, event);
                        },
                        error -> {
                            log.error("[SSE] 复盘分析工作流异常", error);
                            sendSseEvent(emitter, GraphProgressEvent.error(error.getMessage(), threadId));
                            emitter.completeWithError(error);
                        },
                        () -> {
                            log.info("[SSE] 复盘分析工作流完成: threadId={}", threadId);
                            sendSseEvent(emitter, GraphProgressEvent.completeReviewAnalysis(threadId, null));
                            emitter.complete();
                        }
                );
        return emitter;
    }

    /**
     * 配置SSE响应头
     */
    private void configureSseResponse(HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
    }

    /**
     * 发送SSE事件
     */
    private void sendSseEvent(SseEmitter emitter, GraphProgressEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(event));
            log.debug("[SSE] 发送事件: event={}, nodeId={}", event.getEvent(), event.getNodeId());
        } catch (IOException e) {
            log.error("[SSE] 发送失败", e);
            emitter.completeWithError(e);
        }
    }

    /**
     * 判断节点输出是否是中断事件
     */
    private boolean isInterruptionOutput(NodeOutput output) {
        return output instanceof InterruptionMetadata;
    }

    // ===== 私有业务逻辑方法 =====

    /**
     * 查找或创建职位记录
     */
    private String findOrCreateJobPosition(String companyName, String positionTitle, String jdContent) {
        // 1. 查找或创建公司
        Company company = companyService.findByName(companyName)
                .orElseGet(() -> {
                    Company newCompany = new Company();
                    newCompany.setName(companyName);
                    companyService.save(newCompany);
                    return newCompany;
                });
        // 2. 查找是否已存在该职位
        Optional<JobPosition> existing = jobPositionService.findByCompanyIdAndTitle(
                String.valueOf(company.getId()), positionTitle);
        if (existing.isPresent()) {
            log.info("复用已有职位: id={}, company={}, title={}", existing.get().getId(), companyName, positionTitle);
            return String.valueOf(existing.get().getId());
        }
        // 3. 创建新职位
        JobPosition newPosition = new JobPosition();
        newPosition.setCompanyId(String.valueOf(company.getId()));
        newPosition.setTitle(positionTitle);
        newPosition.setJdContent(jdContent);
        jobPositionService.save(newPosition);
        log.info("创建新职位: id={}, company={}, title={}", newPosition.getId(), companyName, positionTitle);
        return String.valueOf(newPosition.getId());
    }

    /**
     * 构建简历上下文（用于 AI 工作流参考）
     * 提取整份简历的所有区块信息
     *
     * @param resumeId 简历ID
     * @return 简历上下文字符串
     */
    private String buildResumeContext(String resumeId) {
        try {
            ResumeDetailVO resume = resumeService.getResumeDetail(resumeId);
            if (resume == null) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            // 目标岗位
            if (resume.getTargetPosition() != null && !resume.getTargetPosition().isBlank()) {
                sb.append("【目标岗位】").append(resume.getTargetPosition()).append("\n");
            }
            // 提取所有区块内容
            if (resume.getSections() != null) {
                for (ResumeDetailVO.ResumeSectionVO section : resume.getSections()) {
                    sb.append("【").append(section.getTitle()).append("】\n");
                    String content = section.getContent();
                    if (content != null && !content.isBlank()) {
                        sb.append(content).append("\n");
                    }
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("构建简历上下文失败: resumeId={}, error={}", resumeId, e.getMessage());
            return "";
        }
    }

    // ===== 转换方法 =====

    private InterviewListItemVO convertToListItemVO(Interview interview) {
        InterviewListItemVO vo = new InterviewListItemVO();
        BeanUtils.copyProperties(interview, vo);

        // 通过 jobPositionId 关联查询公司名和职位名
        if (interview.getJobPositionId() != null) {
            JobPosition jobPosition = jobPositionService.getById(interview.getJobPositionId());
            if (jobPosition != null) {
                vo.setPosition(jobPosition.getTitle());
                Company company = companyService.getById(jobPosition.getCompanyId());
                vo.setCompanyName(company != null ? company.getName() : "");
            }
        }

        vo.setInterviewDate(interview.getDate());
        vo.setRoundType(interview.getRoundType());
        return vo;
    }

    private InterviewDetailVO convertToDetailVO(Interview interview,
                                                 List<InterviewPreparation> preparations,
                                                 InterviewReviewNote reviewNote) {
        InterviewDetailVO vo = new InterviewDetailVO();
        BeanUtils.copyProperties(interview, vo);

        // 通过 jobPositionId 关联查询公司名和职位名
        if (interview.getJobPositionId() != null) {
            JobPosition jobPosition = jobPositionService.getById(interview.getJobPositionId());
            if (jobPosition != null) {
                vo.setPosition(jobPosition.getTitle());
                Company company = companyService.getById(jobPosition.getCompanyId());
                vo.setCompanyName(company != null ? company.getName() : "");
            }
        }

        vo.setInterviewDate(interview.getDate());
        vo.setRoundType(interview.getRoundType());
        vo.setRoundName(interview.getRoundName());
        // 设置关联简历名称
        if (interview.getResumeId() != null && !interview.getResumeId().isBlank()) {
            Resume resume = resumeService.getById(interview.getResumeId());
            if (resume != null) {
                vo.setResumeName(resume.getName());
            }
        }
        vo.setPreparations(preparations.stream().map(this::convertToPreparationVO).collect(Collectors.toList()));
        if (reviewNote != null) {
            vo.setReviewNote(convertToReviewNoteVO(reviewNote));
        }
        return vo;
    }

    private PreparationVO convertToPreparationVO(InterviewPreparation preparation) {
        PreparationVO vo = new PreparationVO();
        BeanUtils.copyProperties(preparation, vo);
        return vo;
    }

    private ReviewNoteVO convertToReviewNoteVO(InterviewReviewNote note) {
        ReviewNoteVO vo = new ReviewNoteVO();
        BeanUtils.copyProperties(note, vo);
        return vo;
    }

    /**
     * 获取同一职位上一轮面试的复盘笔记（JSON格式）
     *
     * @param jobPositionId       职位ID
     * @param currentInterviewId  当前面试ID
     * @return 复盘笔记JSON字符串（包含manual和ai_analysis两种），如果没有则返回null
     */
    private String getPreviousReviewNotesJson(String jobPositionId, String currentInterviewId) {
        if (jobPositionId == null || jobPositionId.isBlank()) {
            return null;
        }
        // 查询同一职位的所有面试，按时间倒序
        List<Interview> interviews = interviewCenterService.list(
                new LambdaQueryWrapper<Interview>()
                        .eq(Interview::getJobPositionId, jobPositionId)
                        .ne(Interview::getId, Long.parseLong(currentInterviewId))
                        .isNotNull(Interview::getDate)
                        .orderByDesc(Interview::getDate)
        );
        if (interviews.isEmpty()) {
            return null;
        }
        // 取最近一场面试
        Interview previousInterview = interviews.get(0);
        // 获取该面试的所有复盘笔记（manual + ai_analysis）
        InterviewReviewNote manualNote = reviewNoteService.getManualNoteByInterviewId(String.valueOf(previousInterview.getId()));
        InterviewReviewNote aiAnalysis = reviewNoteService.getAiAnalysisByInterviewId(String.valueOf(previousInterview.getId()));
        List<Map<String, Object>> notesList = new ArrayList<>();
        if (manualNote != null) {
            Map<String, Object> noteMap = new HashMap<>();
            noteMap.put("type", "manual");
            noteMap.put("weakPoints", manualNote.getWeakPoints());
            noteMap.put("suggestions", manualNote.getSuggestions());
            noteMap.put("lessonsLearned", manualNote.getLessonsLearned());
            notesList.add(noteMap);
        }
        if (aiAnalysis != null) {
            Map<String, Object> noteMap = new HashMap<>();
            noteMap.put("type", "ai_analysis");
            noteMap.put("weakPoints", aiAnalysis.getWeakPoints());
            noteMap.put("suggestions", aiAnalysis.getSuggestions());
            noteMap.put("lessonsLearned", aiAnalysis.getLessonsLearned());
            notesList.add(noteMap);
        }
        if (notesList.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(notesList);
        } catch (Exception e) {
            log.warn("序列化上一轮复盘笔记失败", e);
            return null;
        }
    }

}
