package com.careerforge.interview.voice.gateway;

import com.careerforge.common.enums.InterviewSource;
import com.careerforge.common.enums.InterviewStatus;
import com.careerforge.common.exception.BusinessException;
import com.careerforge.interview.entity.Interview;
import com.careerforge.interview.entity.InterviewSession;
import com.careerforge.interview.handler.InterviewCenterHandler;
import com.careerforge.interview.service.InterviewService;
import com.careerforge.interview.service.InterviewSessionService;
import com.careerforge.interview.voice.dto.*;
import com.careerforge.interview.voice.enums.ControlAction;
import com.careerforge.interview.voice.enums.InterviewPhaseEnum;
import com.careerforge.interview.voice.enums.MessageType;
import com.careerforge.interview.voice.handler.InterviewerAgentHandler;
import com.careerforge.interview.voice.service.QuestionPreGenerateService;
import com.careerforge.interview.voice.service.VoiceSessionManager;
import com.careerforge.jobposition.entity.JobPosition;
import com.careerforge.jobposition.service.JobPositionService;
import com.careerforge.resume.entity.Resume;
import com.careerforge.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Base64;
import java.util.Map;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语音面试网关
 * 核心职责：会话状态管理、角色路由、录音存储
 * 使用 Spring 原生 WebSocket API
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewVoiceGateway {

    private final InterviewerAgentHandler interviewerAgentHandler;
    private final VoiceSessionManager voiceSessionManager;
    private final QuestionPreGenerateService questionPreGenerateService;
    private final InterviewService interviewService;
    private final InterviewCenterHandler interviewCenterHandler;
    private final InterviewSessionService interviewSessionService;
    private final JobPositionService jobPositionService;
    private final ResumeService resumeService;

    // 会话状态：sessionId -> SessionState
    private final Map<String, SessionState> sessionStates = new ConcurrentHashMap<>();

    // WebSocket 会话：sessionId -> WebSocketSession
    private final Map<String, WebSocketSession> wsSessions = new ConcurrentHashMap<>();

    /**
     * 创建语音面试会话
     * 验证面试数据 -> 创建会话记录 -> 初始化内存状态 -> 预生成问题
     *
     * @param request 会话创建请求
     * @return 会话创建结果
     */
    public VoiceSessionCreateVO createSession(VoiceSessionCreateRequest request) {
        // 验证并加载原始面试相关数据
        Interview originalInterview = validateAndGetInterview(request.getInterviewId());
        JobPosition jobPosition = validateAndGetJobPosition(originalInterview);

        // 创建独立的模拟面试 Interview 记录
        Interview mockInterview = createMockInterviewRecord(originalInterview);
        String mockInterviewId = mockInterview.getId();
        log.info("[VoiceGateway] 模拟面试 Interview 已创建, mockInterviewId={}, parentInterviewId={}",
                mockInterviewId, request.getInterviewId());

        String resumeContent = loadResumeContent(originalInterview.getResumeId());

        // 生成会话ID并创建数据库记录（关联到新的模拟面试 Interview）
        String sessionId = generateSessionId();
        InterviewSession session = createSessionRecord(sessionId, mockInterviewId, request, originalInterview, jobPosition);
        log.info("[VoiceGateway] 会话已创建, sessionId={}, mockInterviewId={}, position={}",
                sessionId, mockInterviewId, jobPosition.getTitle());

        // 初始化内存状态（使用新的模拟面试 Interview ID）
        initSessionState(sessionId, mockInterviewId, jobPosition, resumeContent, session);

        // 决定问题生成策略：重新生成 or 复用历史
        boolean regenerate = request.getRegenerateQuestions() == null || request.getRegenerateQuestions();
        if (regenerate) {
            preGenerateQuestions(sessionId, jobPosition, resumeContent, session);
        } else {
            reuseOrGenerateQuestions(sessionId, jobPosition, resumeContent, session, request.getInterviewId());
        }

        // 返回创建结果（interviewId 为新创建的模拟面试 ID）
        return buildCreateResponse(sessionId, mockInterviewId, jobPosition, session);
    }

    /**
     * 验证并获取面试记录
     *
     * @param interviewId 面试ID
     * @return 面试实体
     */
    private Interview validateAndGetInterview(String interviewId) {
        Interview interview = interviewService.getById(interviewId);
        if (interview == null) {
            throw new BusinessException("面试记录不存在");
        }
        return interview;
    }

    /**
     * 验证并获取职位信息
     *
     * @param interview 面试实体
     * @return 职位实体
     */
    private JobPosition validateAndGetJobPosition(Interview interview) {
        if (interview.getJobPositionId() == null || interview.getJobPositionId().isEmpty()) {
            throw new BusinessException("请先关联职位后再开始模拟面试");
        }
        JobPosition jobPosition = jobPositionService.getById(interview.getJobPositionId());
        if (jobPosition == null) {
            throw new BusinessException("关联的职位不存在");
        }
        return jobPosition;
    }

    /**
     * 加载简历内容
     *
     * @param resumeId 简历ID
     * @return 简历Markdown内容，不存在则返回null
     */
    private String loadResumeContent(String resumeId) {
        if (resumeId == null || resumeId.isEmpty()) {
            return null;
        }
        Resume resume = resumeService.getById(resumeId);
        return resume != null ? resume.getMarkdownContent() : null;
    }

    /**
     * 生成会话ID
     *
     * @return 会话ID
     */
    private String generateSessionId() {
        return IdWorker.getIdStr();
    }

    /**
     * 创建模拟面试 Interview 记录（独立于原始真实面试）
     *
     * @param originalInterview 原始面试记录
     * @return 新创建的模拟面试 Interview
     */
    private Interview createMockInterviewRecord(Interview originalInterview) {
        Interview mockInterview = new Interview();
        mockInterview.setId(IdWorker.getIdStr());
        mockInterview.setUserId(originalInterview.getUserId());
        mockInterview.setType(originalInterview.getType());
        mockInterview.setSource(InterviewSource.MOCK.getCode());
        mockInterview.setParentInterviewId(originalInterview.getId());
        mockInterview.setJobPositionId(originalInterview.getJobPositionId());
        mockInterview.setResumeId(originalInterview.getResumeId());
        mockInterview.setJdContent(originalInterview.getJdContent());
        mockInterview.setStatus(InterviewStatus.IN_PROGRESS.getValue());
        interviewService.save(mockInterview);
        return mockInterview;
    }

    /**
     * 创建会话数据库记录
     *
     * @param sessionId 会话ID
     * @param mockInterviewId 模拟面试 Interview ID
     * @param request 创建请求
     * @param interview 原始面试实体
     * @param jobPosition 职位实体
     * @return 会话实体
     */
    private InterviewSession createSessionRecord(String sessionId, String mockInterviewId, VoiceSessionCreateRequest request, Interview interview, JobPosition jobPosition) {
        InterviewSession session = new InterviewSession();
        session.setId(sessionId);
        session.setInterviewId(mockInterviewId);
        session.setUserId(interview.getUserId());
        session.setType(interview.getType());
        session.setPosition(jobPosition.getTitle());
        session.setStatus(InterviewStatus.IN_PROGRESS.getValue());
        session.setCurrentQuestionIndex(0);
        session.setTotalQuestions(request.getTotalQuestions() != null ? request.getTotalQuestions() : 10);
        session.setVoiceMode(request.getVoiceMode() != null ? request.getVoiceMode() : "half_voice");
        session.setAssistCount(0);
        session.setAssistLimit(request.getAssistLimit() != null ? request.getAssistLimit() : 5);
        session.setInterviewerStyle(request.getInterviewerStyle() != null ? request.getInterviewerStyle() : "professional");
        interviewSessionService.save(session);
        return session;
    }

    /**
     * 初始化内存状态
     *
     * @param sessionId 会话ID
     * @param mockInterviewId 模拟面试 Interview ID
     * @param jobPosition 职位实体
     * @param resumeContent 简历内容
     * @param session 会话实体
     */
    private void initSessionState(String sessionId, String mockInterviewId, JobPosition jobPosition, String resumeContent, InterviewSession session) {
        SessionState state = new SessionState();
        state.setInterviewId(mockInterviewId);
        state.setPosition(jobPosition.getTitle());
        state.setJdContent(jobPosition.getJdContent());
        state.setResumeContent(resumeContent);
        state.setTotalQuestions(session.getTotalQuestions());
        state.setAssistRemaining(session.getAssistLimit());
        state.setAssistLimit(session.getAssistLimit());
        state.setInterviewerStyle(session.getInterviewerStyle());
        state.setVoiceMode(session.getVoiceMode());
        sessionStates.put(sessionId, state);
    }

    /**
     * 预生成面试问题
     * 失败不影响会话创建，运行时降级到实时生成
     *
     * @param sessionId 会话ID
     * @param jobPosition 职位实体
     * @param resumeContent 简历内容
     * @param session 会话实体
     */
    private void preGenerateQuestions(String sessionId, JobPosition jobPosition, String resumeContent, InterviewSession session) {
        try {
            PreGenerateContext context = PreGenerateContext.builder()
                    .position(jobPosition.getTitle())
                    .jdContent(jobPosition.getJdContent())
                    .resumeContent(resumeContent)
                    .totalQuestions(session.getTotalQuestions())
                    .interviewerStyle(session.getInterviewerStyle())
                    .build();

            int count = questionPreGenerateService.preGenerateAllQuestions(sessionId, context);
            log.info("[VoiceGateway] 预生成完成, sessionId={}, count={}", sessionId, count);
        } catch (Exception e) {
            log.error("[VoiceGateway] 预生成失败, sessionId={}, 降级到实时生成", sessionId, e);
        }
    }

    /**
     * 尝试复用历史会话的预生成问题，失败则降级到重新生成
     *
     * @param sessionId     新会话ID
     * @param jobPosition   职位实体
     * @param resumeContent 简历内容
     * @param session       新会话实体
     * @param interviewId   关联的面试ID
     */
    private void reuseOrGenerateQuestions(String sessionId, JobPosition jobPosition, String resumeContent, InterviewSession session, String interviewId) {
        try {
            InterviewSession latestSession = interviewSessionService.getLatestSessionWithQuestions(interviewId);
            if (latestSession != null) {
                int result = questionPreGenerateService.copyPreGeneratedQuestions(
                        sessionId,
                        latestSession.getPreGeneratedQuestions(),
                        session.getTotalQuestions()
                );
                if (result > 0) {
                    log.info("[VoiceGateway] 复用历史问题成功, sessionId={}, sourceSessionId={}, count={}",
                            sessionId, latestSession.getId(), result);
                    return;
                }
                log.warn("[VoiceGateway] 历史问题数量不匹配, 期望={}, 降级到重新生成", session.getTotalQuestions());
            } else {
                log.info("[VoiceGateway] 无历史问题可复用, interviewId={}, 降级到重新生成", interviewId);
            }
        } catch (Exception e) {
            log.warn("[VoiceGateway] 复用历史问题异常, 降级到重新生成", e);
        }
        // 降级：重新调用 LLM 生成
        preGenerateQuestions(sessionId, jobPosition, resumeContent, session);
    }

    /**
     * 构建创建结果响应
     *
     * @param sessionId 会话ID
     * @param mockInterviewId 模拟面试 Interview ID
     * @param jobPosition 职位实体
     * @param session 会话实体
     * @return 创建结果VO
     */
    private VoiceSessionCreateVO buildCreateResponse(String sessionId, String mockInterviewId, JobPosition jobPosition, InterviewSession session) {
        return VoiceSessionCreateVO.builder()
                .sessionId(sessionId)
                .interviewId(mockInterviewId)
                .position(jobPosition.getTitle())
                .voiceMode(session.getVoiceMode())
                .totalQuestions(session.getTotalQuestions())
                .assistLimit(session.getAssistLimit())
                .build();
    }

    /**
     * 注册 WebSocket 会话
     * 建立 sessionId 与 WebSocketSession 的映射关系，初始化会话状态
     *
     * @param sessionId 会话ID
     * @param wsSession WebSocket会话对象
     */
    public void registerSession(String sessionId, WebSocketSession wsSession) {
        // 建立 WebSocket 会话映射
        wsSessions.put(sessionId, wsSession);
        // 初始化会话状态（如果不存在）
        sessionStates.putIfAbsent(sessionId, new SessionState());
        log.info("[VoiceGateway] 会话已注册, sessionId={}", sessionId);
    }

    /**
     * 注销 WebSocket 会话
     * 移除会话映射并标记会话为非活跃状态
     *
     * @param sessionId 会话ID
     * @param wsSession WebSocket会话对象
     */
    public void unregisterSession(String sessionId, WebSocketSession wsSession) {
        // 移除 WebSocket 会话映射
        wsSessions.remove(sessionId);
        // 标记会话为非活跃状态
        SessionState state = sessionStates.get(sessionId);
        if (state != null) {
            state.setActive(false);
        }
        log.info("[VoiceGateway] 会话已注销, sessionId={}", sessionId);
    }

    /**
     * 处理候选人音频数据
     * 验证会话状态后转发给面试官 Agent 处理
     *
     * @param sessionId 会话ID
     * @param wsSession WebSocket会话对象
     * @param audioData 音频数据（PCM格式）
     */
    public void handleAudioData(String sessionId, WebSocketSession wsSession, byte[] audioData) {
        SessionState state = sessionStates.get(sessionId);
        // 验证会话存在且活跃
        if (state == null || !state.isActive()) {
            return;
        }

        // 如果会话已冻结，忽略音频数据
        if (state.isFrozen()) {
            log.debug("[VoiceGateway] 会话已冻结，忽略音频, sessionId={}", sessionId);
            return;
        }

        // 转发给面试官 Agent 处理（ASR识别 -> AI生成回复 -> TTS合成）
        interviewerAgentHandler.handleCandidateAudio(sessionId, audioData)
                .subscribe(
                        response -> sendResponse(sessionId, response),
                        error -> log.error("[VoiceGateway] 处理音频出错, sessionId={}", sessionId, error),
                        () -> log.debug("[VoiceGateway] 音频处理完成, sessionId={}", sessionId)
                );
    }

    /**
     * 处理会话错误
     * 记录错误日志并标记会话为非活跃状态
     *
     * @param sessionId 会话ID
     * @param wsSession WebSocket会话对象
     * @param error 错误对象
     */
    public void handleSessionError(String sessionId, WebSocketSession wsSession, Throwable error) {
        log.error("[VoiceGateway] 会话错误, sessionId={}", sessionId, error);
        // 标记会话为非活跃状态
        SessionState state = sessionStates.get(sessionId);
        if (state != null) {
            state.setActive(false);
        }
    }

    /**
     * 冻结面试
     * 暂停面试进程，忽略后续音频输入，但仍允许求助操作
     *
     * @param sessionId 会话ID
     */
    public void freezeInterview(String sessionId) {
        SessionState state = sessionStates.get(sessionId);
        if (state != null) {
            // 标记会话为冻结状态
            state.setFrozen(true);
            state.setFreezeTime(System.currentTimeMillis());
            log.info("[VoiceGateway] 面试已冻结, sessionId={}", sessionId);

            // 通知客户端状态变更
            sendResponse(sessionId, VoiceResponse.state(VoiceResponse.StateData.builder()
                    .state(InterviewSessionState.FROZEN.getCode())
                    .currentQuestion(state.getCurrentQuestion())
                    .totalQuestions(state.getTotalQuestions())
                    .assistRemaining(state.getAssistRemaining())
                    .elapsedTime(state.getElapsedTime())
                    .build()));
        }
    }

    /**
     * 恢复面试
     * 解除冻结状态，恢复音频处理
     *
     * @param sessionId 会话ID
     */
    public void resumeInterview(String sessionId) {
        SessionState state = sessionStates.get(sessionId);
        if (state != null) {
            // 解除冻结状态
            state.setFrozen(false);
            log.info("[VoiceGateway] 面试已恢复, sessionId={}", sessionId);

            // 通知客户端状态变更
            sendResponse(sessionId, VoiceResponse.state(VoiceResponse.StateData.builder()
                    .state(InterviewSessionState.INTERVIEWING.getCode())
                    .currentQuestion(state.getCurrentQuestion())
                    .totalQuestions(state.getTotalQuestions())
                    .assistRemaining(state.getAssistRemaining())
                    .elapsedTime(state.getElapsedTime())
                    .build()));
        }
    }

    /**
     * 结束面试
     * 标记会话为已完成状态，保存对话文本，触发异步复盘分析
     *
     * @param sessionId 会话ID
     */
    public void endInterview(String sessionId) {
        SessionState state = sessionStates.get(sessionId);
        if (state == null) {
            return;
        }
        state.setActive(false);
        state.setCompleted(true);
        String interviewId = state.getInterviewId();
        log.info("[VoiceGateway] 面试已结束, sessionId={}, interviewId={}", sessionId, interviewId);
        // 保存对话文本并触发异步复盘分析
        saveInterviewResult(sessionId, interviewId);
        // 通知客户端状态变更
        sendResponse(sessionId, VoiceResponse.state(VoiceResponse.StateData.builder()
                .state(InterviewSessionState.COMPLETED.getCode())
                .currentQuestion(state.getCurrentQuestion())
                .totalQuestions(state.getTotalQuestions())
                .assistRemaining(state.getAssistRemaining())
                .elapsedTime(state.getElapsedTime())
                .build()));
    }

    /**
     * 保存面试结果并触发异步复盘分析
     */
    private void saveInterviewResult(String sessionId, String interviewId) {
        try {
            String conversationHistory = interviewerAgentHandler.getFullConversationHistory(sessionId);
            // 保存 transcript 到 Interview
            if (interviewId != null && !conversationHistory.isEmpty()) {
                Interview interview = interviewService.getById(interviewId);
                if (interview != null) {
                    interview.setTranscript(conversationHistory);
                    interview.setStatus(InterviewStatus.COMPLETED.getValue());
                    interviewService.updateById(interview);
                    log.info("[VoiceGateway] 对话文本已保存, interviewId={}, length={}", interviewId, conversationHistory.length());
                    // 传递已查询的 Interview 对象，避免重复查询
                    interviewCenterHandler.autoReviewAnalysis(interview);
                    log.info("[VoiceGateway] 已触发异步复盘分析, interviewId={}", interviewId);
                }
            }
            // 更新 InterviewSession 状态
            InterviewSession sessionRecord = interviewSessionService.getById(sessionId);
            if (sessionRecord != null) {
                sessionRecord.setStatus(InterviewStatus.COMPLETED.getValue());
                interviewSessionService.updateById(sessionRecord);
            }
        } catch (Exception e) {
            log.error("[VoiceGateway] 面试结束后处理失败, sessionId={}", sessionId, e);
        }
    }

    /**
     * 获取会话状态
     * 返回当前会话的完整状态信息（状态码、当前题号、求助次数等）
     *
     * @param sessionId 会话ID
     * @return 会话状态数据
     */
    public VoiceResponse.StateData getSessionState(String sessionId) {
        SessionState state = sessionStates.get(sessionId);
        // 会话不存在
        if (state == null) {
            return VoiceResponse.StateData.builder()
                    .state(InterviewSessionState.NOT_FOUND.getCode())
                    .build();
        }

        // 返回当前会话状态
        return VoiceResponse.StateData.builder()
                .state(state.getStateCode())
                .currentQuestion(state.getCurrentQuestion())
                .totalQuestions(state.getTotalQuestions())
                .assistRemaining(state.getAssistRemaining())
                .elapsedTime(state.getElapsedTime())
                .build();
    }

    /**
     * 发送响应消息到指定会话
     * 通过 WebSocket 向客户端推送响应
     *
     * @param sessionId 会话ID
     * @param response 响应对象
     */
    public void sendResponse(String sessionId, VoiceResponse response) {
        WebSocketSession wsSession = wsSessions.get(sessionId);
        // 验证会话存在且连接正常
        if (wsSession != null && wsSession.isOpen()) {
            voiceSessionManager.sendResponse(wsSession, response);
        }
    }

    /**
     * 广播响应消息到所有会话
     * 向所有活跃的 WebSocket 连接推送消息
     *
     * @param response 响应对象
     */
    public void broadcastResponse(VoiceResponse response) {
        voiceSessionManager.broadcastAll(response);
    }

    /**
     * 处理音频请求
     * 解析 Base64 编码的音频数据并转发给音频处理器
     *
     * @param sessionId 会话ID
     * @param request 音频请求对象（包含 Base64 编码的音频数据、格式、采样率）
     */
    public void handleAudioRequest(String sessionId, VoiceRequest<VoiceRequest.AudioData> request) {
        try {
            VoiceRequest.AudioData audioData = request.getData();
            String audioBase64 = audioData.getAudio();
            // 获取音频格式，默认为 PCM
            String format = audioData.getFormat() != null ? audioData.getFormat() : "pcm";
            // 获取采样率，默认为 16kHz
            Integer sampleRate = audioData.getSampleRate() != null ? audioData.getSampleRate() : 16000;

            // 解码 Base64 音频数据
            byte[] audioDataBytes = Base64.getDecoder().decode(audioBase64);

            // 转发给音频处理器进行 ASR 识别
            handleAudioData(sessionId, wsSessions.get(sessionId), audioDataBytes);

        } catch (Exception e) {
            log.error("[VoiceGateway] 处理音频请求失败, sessionId={}", sessionId, e);
        }
    }

    /**
     * 处理控制请求
     * 路由不同的控制动作（START/STOP/END/FREEZE/RESUME）到对应的处理方法
     *
     * @param sessionId 会话ID
     * @param request 控制请求对象（包含动作类型和参数）
     */
    public void handleControlRequest(String sessionId, VoiceRequest<VoiceRequest.ControlData> request) {
        try {
            VoiceRequest.ControlData controlData = request.getData();
            String actionCode = controlData.getAction();
            // 解析控制动作枚举
            ControlAction action = ControlAction.fromCode(actionCode);

            if (action == null) {
                log.warn("[VoiceGateway] 未知控制操作: {}", actionCode);
                return;
            }

            // 路由到对应的处理方法
            switch (action) {
                case START:
                    handleStartAction(sessionId, controlData);
                    break;
                case STOP:
                    handleStopAction(sessionId);
                    break;
                case END:
                    endInterview(sessionId);
                    break;
                case FREEZE:
                    freezeInterview(sessionId);
                    break;
                case RESUME:
                    resumeInterview(sessionId);
                    break;
                default:
                    log.warn("[VoiceGateway] 未处理的控制操作: {}", action);
            }
        } catch (Exception e) {
            log.error("[VoiceGateway] 处理控制请求失败, sessionId={}", sessionId, e);
        }
    }

    /**
     * 处理开始动作
     * 初始化会话状态，请求候选人进行自我介绍
     *
     * @param sessionId 会话ID
     * @param controlData 控制数据（可能包含扩展参数）
     */
    private void handleStartAction(String sessionId, VoiceRequest.ControlData controlData) {
        SessionState state = sessionStates.computeIfAbsent(sessionId, k -> new SessionState());

        // 设置初始状态
        state.setActive(true);
        state.setFrozen(false);
        state.setStartTime(System.currentTimeMillis());
        state.setPhase(InterviewPhaseEnum.WAITING_SELF_INTRODUCTION);

        // 处理扩展参数（题目总数、求助次数限制）
        Object params = controlData.getParams();
        if (params instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> paramMap = (Map<String, Object>) params;
            if (paramMap.containsKey("totalQuestions")) {
                state.setTotalQuestions((Integer) paramMap.get("totalQuestions"));
            }
            if (paramMap.containsKey("assistLimit")) {
                state.setAssistRemaining((Integer) paramMap.get("assistLimit"));
            }
        }

        log.info("[VoiceGateway] 面试开始, sessionId={}", sessionId);

        // 通知客户端面试开始
        sendResponse(sessionId, VoiceResponse.state(VoiceResponse.StateData.builder()
                .state(InterviewSessionState.INTERVIEWING.getCode())
                .currentQuestion(state.getCurrentQuestion())
                .totalQuestions(state.getTotalQuestions())
                .assistRemaining(state.getAssistRemaining())
                .elapsedTime(0)
                .build()));

        // 请求候选人自我介绍
        interviewerAgentHandler.requestSelfIntroduction(sessionId)
                .subscribe(
                        response -> sendResponse(sessionId, response),
                        error -> log.error("[VoiceGateway] 请求自我介绍失败, sessionId={}", sessionId, error),
                        () -> log.debug("[VoiceGateway] 自我介绍请求完成, sessionId={}", sessionId)
                );
    }

    /**
     * 处理停止动作
     * 暂停录音，会话保持活跃状态
     *
     * @param sessionId 会话ID
     */
    private void handleStopAction(String sessionId) {
        SessionState state = sessionStates.get(sessionId);
        if (state != null) {
            log.info("[VoiceGateway] 录音停止, sessionId={}", sessionId);
        }
    }

    /**
     * 获取会话状态对象（供内部使用）
     * 用于其他组件访问当前会话的内存状态
     *
     * @param sessionId 会话ID
     * @return 会话状态对象，不存在则返回 null
     */
    public SessionState getInternalState(String sessionId) {
        return sessionStates.get(sessionId);
    }
}
