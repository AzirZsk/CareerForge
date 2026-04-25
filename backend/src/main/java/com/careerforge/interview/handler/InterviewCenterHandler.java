package com.careerforge.interview.handler;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.careerforge.common.enums.InterviewSource;
import com.careerforge.common.enums.InterviewStatus;
import com.careerforge.common.exception.BusinessException;
import com.careerforge.common.util.SecurityUtils;
import com.careerforge.common.response.PageResponse;
import com.careerforge.company.entity.Company;
import com.careerforge.company.service.CompanyService;
import com.careerforge.interview.dto.interviewcenter.*;
import com.careerforge.interview.entity.Interview;
import com.careerforge.interview.entity.InterviewPreparation;
import com.careerforge.interview.entity.InterviewReviewNote;
import com.careerforge.interview.entity.InterviewAIAnalysis;
import com.careerforge.interview.entity.InterviewSession;
import com.careerforge.interview.graph.preparation.InterviewPreparationGraphService;
import com.careerforge.interview.graph.review.ReviewAnalysisGraphService;
import com.careerforge.interview.service.InterviewCenterService;
import com.careerforge.interview.service.InterviewPreparationService;
import com.careerforge.interview.service.InterviewReviewNoteService;
import com.careerforge.interview.service.InterviewAIAnalysisService;
import com.careerforge.interview.service.InterviewSessionService;
import com.careerforge.interview.voice.entity.RecordingIndex;
import com.careerforge.interview.voice.mapper.RecordingIndexMapper;
import com.careerforge.jobposition.entity.JobPosition;
import com.careerforge.jobposition.service.JobPositionService;
import com.careerforge.resume.dto.ResumeDetailVO;
import com.careerforge.resume.entity.Resume;
import com.careerforge.resume.service.ResumeService;
import com.careerforge.task.entity.AsyncTask;
import com.careerforge.task.enums.TaskType;
import com.careerforge.task.service.AsyncTaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// 使用完整限定名避免常量冲突
import com.careerforge.interview.graph.preparation.InterviewPreparationGraphConstants;
import com.careerforge.interview.graph.review.ReviewAnalysisGraphConstants;

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

    // 异步复盘分析任务管理（interviewId -> Disposable）
    private final ConcurrentHashMap<String, Disposable> activeAnalyses = new ConcurrentHashMap<>();

    private final InterviewCenterService interviewCenterService;
    private final InterviewPreparationService preparationService;
    private final InterviewReviewNoteService reviewNoteService;
    private final InterviewAIAnalysisService aiAnalysisService;
    private final InterviewSessionService interviewSessionService;
    private final RecordingIndexMapper recordingIndexMapper;
    private final JobPositionService jobPositionService;
    private final CompanyService companyService;
    private final InterviewPreparationGraphService preparationGraphService;
    private final ReviewAnalysisGraphService reviewGraphService;
    private final ResumeService resumeService;
    private final AsyncTaskService asyncTaskService;
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
        interview.setUserId(SecurityUtils.getCurrentUserId());
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
            interview.setStatus(InterviewStatus.COMPLETED.getValue());
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
        Map<String, Object> initialState = buildReviewAnalysisInitialState(id);
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
     * 自动触发复盘分析（异步后台执行，通过通知中心通知用户）
     *
     * @param interview 模拟面试 Interview 对象（由调用方传入，避免重复查询）
     */
    public void autoReviewAnalysis(Interview interview) {
        String interviewId = interview.getId();
        log.info("[AutoReview] 开始异步复盘分析: interviewId={}", interviewId);

        if (interview.getUserId() == null) {
            log.error("[AutoReview] 面试记录缺少用户ID: interviewId={}", interviewId);
            return;
        }

        // 创建异步任务
        AsyncTask task = asyncTaskService.createTask(interview.getUserId(), TaskType.REVIEW_ANALYSIS, interviewId);
        String taskId = task.getId();
        log.info("[AutoReview] 异步任务已创建: taskId={}, interviewId={}", taskId, interviewId);

        // 构建工作流初始状态
        String threadId = UUID.randomUUID().toString();
        Map<String, Object> initialState = buildReviewAnalysisInitialState(interviewId);

        // 异步执行复盘分析工作流
        Disposable disposable = reviewGraphService.streamAnalysis(initialState, threadId)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        output -> {
                            String node = output.node();
                            asyncTaskService.markRunning(taskId);
                            // 根据节点更新进度
                            if (node.contains("analyze_transcript")) {
                                asyncTaskService.updateProgress(taskId, 33, "对话分析完成");
                            } else if (node.contains("analyze_interview")) {
                                asyncTaskService.updateProgress(taskId, 66, "面试分析完成");
                            } else if (node.contains("generate_advice")) {
                                asyncTaskService.updateProgress(taskId, 90, "正在生成改进建议");
                            }
                            log.debug("[AutoReview] 节点完成: node={}, interviewId={}", node, interviewId);
                        },
                        error -> {
                            log.error("[AutoReview] 复盘分析工作流异常: interviewId={}", interviewId, error);
                            asyncTaskService.markFailed(taskId, error.getMessage());
                            activeAnalyses.remove(interviewId);
                        },
                        () -> {
                            log.info("[AutoReview] 复盘分析工作流完成: interviewId={}, taskId={}", interviewId, taskId);
                            asyncTaskService.markCompleted(taskId, "复盘分析完成");
                            activeAnalyses.remove(interviewId);
                        }
                );
        activeAnalyses.put(interviewId, disposable);
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

    /**
     * 构建复盘分析工作流初始状态
     */
    private Map<String, Object> buildReviewAnalysisInitialState(String interviewId) {
        Map<String, Object> initialState = new HashMap<>();
        initialState.put(ReviewAnalysisGraphConstants.STATE_INTERVIEW_ID, interviewId);
        initialState.put(ReviewAnalysisGraphConstants.STATE_MESSAGES, new ArrayList<String>());
        return initialState;
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

    /**
     * 获取真实面试关联的模拟面试历史列表
     */
    public List<MockSessionSummaryVO> getMockSessions(String interviewId) {
        LambdaQueryWrapper<Interview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Interview::getParentInterviewId, interviewId)
                .eq(Interview::getSource, InterviewSource.MOCK.getCode())
                .eq(Interview::getStatus, InterviewStatus.COMPLETED.getValue())
                .orderByDesc(Interview::getCreatedAt);
        List<Interview> mockInterviews = interviewCenterService.list(wrapper);
        return mockInterviews.stream().map(this::convertToMockSessionSummary).collect(Collectors.toList());
    }

    /**
     * 获取模拟面试详情（含 AI 分析结果）
     */
    public MockSessionDetailVO getMockSessionDetail(String mockInterviewId) {
        Interview mockInterview = interviewCenterService.getById(mockInterviewId);
        if (mockInterview == null) {
            throw new BusinessException("模拟面试记录不存在");
        }
        MockSessionDetailVO detail = new MockSessionDetailVO();
        detail.setSummary(convertToMockSessionSummary(mockInterview));
        // 查询 AI 分析
        InterviewAIAnalysis aiAnalysis = aiAnalysisService.getByInterviewId(mockInterviewId);
        if (aiAnalysis != null) {
            detail.setAiAnalysis(convertToAIAnalysisVO(aiAnalysis));
        }
        // 查询 sessionId
        InterviewSession session = findSessionByInterviewId(mockInterviewId);
        if (session != null) {
            detail.setSessionId(String.valueOf(session.getId()));
        }
        return detail;
    }

    /**
     * 转换 Interview 为 MockSessionSummaryVO
     */
    private MockSessionSummaryVO convertToMockSessionSummary(Interview mockInterview) {
        MockSessionSummaryVO vo = new MockSessionSummaryVO();
        vo.setId(String.valueOf(mockInterview.getId()));
        vo.setCreatedAt(mockInterview.getCreatedAt());
        vo.setDuration(mockInterview.getDuration());
        vo.setQuestions(mockInterview.getQuestions());
        // 查询 InterviewSession 获取会话详情
        InterviewSession session = findSessionByInterviewId(String.valueOf(mockInterview.getId()));
        if (session != null) {
            vo.setSessionId(String.valueOf(session.getId()));
            vo.setInterviewerStyle(session.getInterviewerStyle());
            vo.setVoiceMode(session.getVoiceMode());
            vo.setAssistCount(session.getAssistCount());
            vo.setAssistLimit(session.getAssistLimit());
        }
        // 查询 AI 分析摘要
        InterviewAIAnalysis aiAnalysis = aiAnalysisService.getByInterviewId(String.valueOf(mockInterview.getId()));
        if (aiAnalysis != null) {
            vo.setHasAnalysis(true);
            var interviewAnalysis = aiAnalysisService.parseInterviewAnalysis(aiAnalysis);
            if (interviewAnalysis != null) {
                vo.setOverallScore(interviewAnalysis.getOverallScore());
                vo.setOverallPerformance(interviewAnalysis.getOverallPerformance());
                vo.setJdMatchScore(interviewAnalysis.getJdMatchScore());
            }
        } else {
            vo.setHasAnalysis(false);
        }
        // 判断是否有录音
        vo.setHasRecording(checkHasRecording(vo.getSessionId()));
        return vo;
    }

    /**
     * 通过 interviewId 查找 InterviewSession
     */
    private InterviewSession findSessionByInterviewId(String interviewId) {
        LambdaQueryWrapper<InterviewSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewSession::getInterviewId, interviewId);
        return interviewSessionService.getOne(wrapper, false);
    }

    /**
     * 检查是否有录音
     */
    private boolean checkHasRecording(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return false;
        }
        LambdaQueryWrapper<RecordingIndex> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecordingIndex::getSessionId, sessionId);
        return recordingIndexMapper.selectCount(wrapper) > 0;
    }

}
