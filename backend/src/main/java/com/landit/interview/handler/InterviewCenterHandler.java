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
import com.landit.interview.entity.InterviewAIAnalysis;
import com.landit.interview.graph.preparation.InterviewPreparationGraphService;
import com.landit.interview.graph.review.ReviewAnalysisGraphService;
import com.landit.interview.service.InterviewCenterService;
import com.landit.interview.service.InterviewPreparationService;
import com.landit.interview.service.InterviewReviewNoteService;
import com.landit.interview.service.InterviewAIAnalysisService;
import com.landit.interview.voice.dto.FileASRResult;
import com.landit.interview.voice.service.FileASRService;
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
import org.springframework.web.multipart.MultipartFile;
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
    private final InterviewAIAnalysisService aiAnalysisService;
    private final JobPositionService jobPositionService;
    private final CompanyService companyService;
    private final InterviewPreparationGraphService preparationGraphService;
    private final ReviewAnalysisGraphService reviewGraphService;
    private final ResumeService resumeService;
    private final FileASRService fileASRService;
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
        InterviewReviewNote reviewNote = reviewNoteService.getByInterviewId(id);
        InterviewAIAnalysis aiAnalysis = aiAnalysisService.getByInterviewId(id);
        return convertToDetailVO(interview, preparations, reviewNote, aiAnalysis);
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
            // 手动设置面试结果后，自动将状态修改为已完成
            interview.setStatus("completed");
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
        if (request.getTranscript() != null) {
            interview.setTranscript(request.getTranscript());
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
        configureSseResponse(response);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        String threadId = UUID.randomUUID().toString();
        // 构建初始状态：仅传 interviewId 和 messages，其他数据由 Node 通过 ContextBuilder 获取
        Map<String, Object> initialState = new HashMap<>();
        initialState.put(InterviewPreparationGraphConstants.STATE_INTERVIEW_ID, id);
        initialState.put(InterviewPreparationGraphConstants.STATE_MESSAGES, new ArrayList<String>());
        // 发送开始事件
        sendSseEvent(emitter, GraphProgressEvent.startPreparation(id, threadId));
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
     * @param transcript        面试过程文字记录（用户输入，持久化到 Interview 表）
     * @param response          HTTP响应
     * @return SSE事件发射器
     */
    public SseEmitter streamReviewAnalysis(String id, String transcript, HttpServletResponse response) {
        log.info("开始SSE流式复盘分析: interviewId={}", id);
        // 如果传入了面试过程文字记录，先保存到 Interview 表
        if (transcript != null && !transcript.isBlank()) {
            Interview interview = interviewCenterService.getById(id);
            if (interview != null) {
                interview.setTranscript(transcript);
                interviewCenterService.updateById(interview);
                log.info("保存面试过程文字记录: interviewId={}", id);
            }
        }
        configureSseResponse(response);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        String threadId = UUID.randomUUID().toString();
        // 构建初始状态：仅传 interviewId 和 messages
        Map<String, Object> initialState = new HashMap<>();
        initialState.put(ReviewAnalysisGraphConstants.STATE_INTERVIEW_ID, id);
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
                            // 数据已由 GenerateAdviceNode 保存，无需在此重复保存
                            sendSseEvent(emitter, GraphProgressEvent.completeReviewAnalysis(threadId, Collections.emptyList()));
                            emitter.complete();
                        }
                );
        return emitter;
    }

    /**
     * 流式转录音频文件
     * 用于复盘场景，上传面试录音自动转文字
     *
     * @param id       面试ID
     * @param file     音频文件
     * @param response HTTP响应
     * @return SSE事件发射器
     */
    public SseEmitter transcribeAudio(String id, MultipartFile file, HttpServletResponse response) {
        log.info("开始SSE流式音频转录: interviewId={}, filename={}, size={}",
                id, file.getOriginalFilename(), file.getSize());
        // 验证面试存在
        getInterviewDetail(id);
        configureSseResponse(response);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        fileASRService.transcribe(file)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        result -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("transcribe")
                                        .data(objectMapper.writeValueAsString(result)));
                                log.debug("[SSE] 发送转录事件: status={}, progress={}",
                                        result.getStatus(), result.getProgress());
                            } catch (IOException e) {
                                log.error("[SSE] 发送转录事件失败", e);
                            }
                        },
                        error -> {
                            log.error("[SSE] 音频转录失败", error);
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("transcribe")
                                        .data(objectMapper.writeValueAsString(
                                                FileASRResult.failed(error.getMessage()))));
                            } catch (IOException ignored) {
                            }
                            emitter.completeWithError(error);
                        },
                        () -> {
                            log.info("[SSE] 音频转录完成: interviewId={}", id);
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
                                                 InterviewReviewNote reviewNote,
                                                 InterviewAIAnalysis aiAnalysis) {
        InterviewDetailVO vo = new InterviewDetailVO();
        BeanUtils.copyProperties(interview, vo);

        // 通过 jobPositionId 关联查询公司名、职位名、调研数据、分析数据
        if (interview.getJobPositionId() != null) {
            JobPosition jobPosition = jobPositionService.getById(interview.getJobPositionId());
            if (jobPosition != null) {
                vo.setPosition(jobPosition.getTitle());
                // 填充 JD 分析数据（从 JobPosition 表获取）
                vo.setJdAnalysis(jobPosition.getJdAnalysis());
                Company company = companyService.getById(jobPosition.getCompanyId());
                vo.setCompanyName(company != null ? company.getName() : "");
                // 填充公司调研数据（从 Company 表获取）
                if (company != null && company.getResearch() != null) {
                    vo.setCompanyResearch(company.getResearch());
                }
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
        if (aiAnalysis != null) {
            vo.setAiAnalysisNote(convertToAIAnalysisVO(aiAnalysis));
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

    private AIAnalysisVO convertToAIAnalysisVO(InterviewAIAnalysis aiAnalysis) {
        AIAnalysisVO vo = new AIAnalysisVO();
        vo.setId(String.valueOf(aiAnalysis.getId()));
        vo.setInterviewId(aiAnalysis.getInterviewId());
        vo.setAdviceList(aiAnalysisService.parseAdviceList(aiAnalysis));
        vo.setTranscriptAnalysis(aiAnalysisService.parseTranscriptAnalysis(aiAnalysis));
        vo.setInterviewAnalysis(aiAnalysisService.parseInterviewAnalysis(aiAnalysis));
        vo.setCreatedAt(aiAnalysis.getCreatedAt());
        vo.setUpdatedAt(aiAnalysis.getUpdatedAt());
        return vo;
    }

}
