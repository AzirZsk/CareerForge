package com.landit.interview.voice.gateway;

import com.landit.common.exception.BusinessException;
import com.landit.interview.entity.Interview;
import com.landit.interview.entity.InterviewSession;
import com.landit.interview.mapper.InterviewMapper;
import com.landit.interview.mapper.InterviewSessionMapper;
import com.landit.interview.voice.dto.*;
import com.landit.interview.voice.enums.ControlAction;
import com.landit.interview.voice.enums.InterviewPhaseEnum;
import com.landit.interview.voice.enums.MessageType;
import com.landit.interview.voice.handler.AssistantAgentHandler;
import com.landit.interview.voice.handler.InterviewerAgentHandler;
import com.landit.interview.voice.service.QuestionPreGenerateService;
import com.landit.interview.voice.service.RecordingService;
import com.landit.interview.voice.service.TTSService;
import com.landit.interview.voice.service.VoiceServiceFactory;
import com.landit.interview.voice.service.VoiceSessionManager;
import com.landit.jobposition.entity.JobPosition;
import com.landit.jobposition.mapper.JobPositionMapper;
import com.landit.resume.entity.Resume;
import com.landit.resume.mapper.ResumeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
    private final AssistantAgentHandler assistantAgentHandler;
    private final RecordingService recordingService;
    private final VoiceSessionManager voiceSessionManager;
    private final VoiceServiceFactory voiceServiceFactory;
    private final QuestionPreGenerateService questionPreGenerateService;
    private final InterviewMapper interviewMapper;
    private final InterviewSessionMapper interviewSessionMapper;
    private final JobPositionMapper jobPositionMapper;
    private final ResumeMapper resumeMapper;

    // 会话状态：sessionId -> SessionState
    private final Map<String, SessionState> sessionStates = new ConcurrentHashMap<>();

    // WebSocket 会话：sessionId -> WebSocketSession
    private final Map<String, WebSocketSession> wsSessions = new ConcurrentHashMap<>();

    public VoiceSessionCreateVO createSession(VoiceSessionCreateRequest request) {
        // 1. 查询关联的真实面试
        Interview interview = interviewMapper.selectById(request.getInterviewId());
        if (interview == null) {
            throw new BusinessException("面试记录不存在");
        }

        // 2. 检查是否关联职位
        if (interview.getJobPositionId() == null || interview.getJobPositionId().isEmpty()) {
            throw new BusinessException("请先关联职位后再开始模拟面试");
        }

        // 3. 获取职位信息（JD）
        JobPosition jobPosition = jobPositionMapper.selectById(interview.getJobPositionId());
        if (jobPosition == null) {
            throw new BusinessException("关联的职位不存在");
        }

        // 4. 获取简历内容（如果有）
        String resumeContent = null;
        if (interview.getResumeId() != null && !interview.getResumeId().isEmpty()) {
            Resume resume = resumeMapper.selectById(interview.getResumeId());
            if (resume != null) {
                resumeContent = resume.getMarkdownContent();
            }
        }

        // 5. 生成会话 ID
        String sessionId = UUID.randomUUID().toString().replace("-", "");

        // 6. 创建数据库记录
        InterviewSession session = new InterviewSession();
        session.setId(sessionId);
        session.setInterviewId(request.getInterviewId());
        session.setUserId(interview.getUserId());
        session.setType(interview.getType());
        session.setPosition(jobPosition.getTitle());
        session.setStatus("in_progress");
        session.setCurrentQuestionIndex(0);
        session.setTotalQuestions(request.getTotalQuestions() != null ? request.getTotalQuestions() : 10);
        session.setVoiceMode(request.getVoiceMode() != null ? request.getVoiceMode() : "half_voice");
        session.setAssistCount(0);
        session.setAssistLimit(request.getAssistLimit() != null ? request.getAssistLimit() : 5);
        session.setInterviewerStyle(request.getInterviewerStyle() != null ? request.getInterviewerStyle() : "professional");
        interviewSessionMapper.insert(session);

        // 7. 初始化内存状态（包含 JD 和简历上下文）
        SessionState state = new SessionState();
        state.setInterviewId(request.getInterviewId());
        state.setPosition(jobPosition.getTitle());
        state.setJdContent(jobPosition.getJdContent());
        state.setResumeContent(resumeContent);
        state.setTotalQuestions(session.getTotalQuestions());
        state.setAssistRemaining(session.getAssistLimit());
        state.setAssistLimit(session.getAssistLimit());
        state.setInterviewerStyle(session.getInterviewerStyle());
        state.setVoiceMode(session.getVoiceMode());
        sessionStates.put(sessionId, state);

        log.info("[VoiceGateway] Session created, sessionId={}, interviewId={}, position={}",
                sessionId, request.getInterviewId(), jobPosition.getTitle());

        // 8. 同步预生成问题（用户需等待 5-10 秒）
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
            // 预生成失败不影响会话创建，运行时会降级到实时生成
        }

        // 9. 返回创建结果
        return VoiceSessionCreateVO.builder()
                .sessionId(sessionId)
                .interviewId(request.getInterviewId())
                .position(jobPosition.getTitle())
                .voiceMode(session.getVoiceMode())
                .totalQuestions(session.getTotalQuestions())
                .assistLimit(session.getAssistLimit())
                .build();
    }

    public void registerSession(String sessionId, WebSocketSession wsSession) {
        wsSessions.put(sessionId, wsSession);
        sessionStates.putIfAbsent(sessionId, new SessionState());
        log.info("[VoiceGateway] Session registered, sessionId={}", sessionId);
    }

    public void unregisterSession(String sessionId, WebSocketSession wsSession) {
        wsSessions.remove(sessionId);
        SessionState state = sessionStates.get(sessionId);
        if (state != null) {
            state.setActive(false);
        }
        log.info("[VoiceGateway] Session unregistered, sessionId={}", sessionId);
    }

    public void handleRequest(String sessionId, WebSocketSession wsSession, VoiceRequest<?> request) {
        SessionState state = sessionStates.get(sessionId);
        if (state == null) {
            log.warn("[VoiceGateway] Session not found, sessionId={}", sessionId);
            return;
        }

        String typeStr = request.getType();
        MessageType messageType = MessageType.fromCode(typeStr);
        log.debug("[VoiceGateway] Handling request, sessionId={}, type={}", sessionId, typeStr);

        switch (messageType) {
            case AUDIO:
                handleAudioRequest(sessionId, (VoiceRequest<VoiceRequest.AudioData>) request);
                break;
            case CONTROL:
                handleControlRequest(sessionId, (VoiceRequest<VoiceRequest.ControlData>) request);
                break;
            default:
                log.warn("[VoiceGateway] Unknown request type: {}", typeStr);
        }
    }

    public void handleAudioData(String sessionId, WebSocketSession wsSession, byte[] audioData) {
        SessionState state = sessionStates.get(sessionId);
        if (state == null || !state.isActive()) {
            return;
        }

        // 如果会话已冻结，忽略音频
        if (state.isFrozen()) {
            log.debug("[VoiceGateway] Session frozen, ignoring audio, sessionId={}", sessionId);
            return;
        }

        // 转发给面试官 Agent 处理
        interviewerAgentHandler.handleCandidateAudio(sessionId, audioData)
                .subscribe(
                        response -> sendResponse(sessionId, response),
                        error -> log.error("[VoiceGateway] Error handling audio, sessionId={}", sessionId, error),
                        () -> log.debug("[VoiceGateway] Audio processing completed, sessionId={}", sessionId)
                );
    }

    public void handleSessionError(String sessionId, WebSocketSession wsSession, Throwable error) {
        log.error("[VoiceGateway] Session error, sessionId={}", sessionId, error);
        SessionState state = sessionStates.get(sessionId);
        if (state != null) {
            state.setActive(false);
        }
    }

    public void freezeInterview(String sessionId) {
        SessionState state = sessionStates.get(sessionId);
        if (state != null) {
            state.setFrozen(true);
            state.setFreezeTime(System.currentTimeMillis());
            log.info("[VoiceGateway] Interview frozen, sessionId={}", sessionId);

            // 通知客户端
            sendResponse(sessionId, VoiceResponse.state(VoiceResponse.StateData.builder()
                    .state(InterviewSessionState.FROZEN.getCode())
                    .currentQuestion(state.getCurrentQuestion())
                    .totalQuestions(state.getTotalQuestions())
                    .assistRemaining(state.getAssistRemaining())
                    .elapsedTime(state.getElapsedTime())
                    .build()));
        }
    }

    public void resumeInterview(String sessionId) {
        SessionState state = sessionStates.get(sessionId);
        if (state != null) {
            state.setFrozen(false);
            log.info("[VoiceGateway] Interview resumed, sessionId={}", sessionId);

            // 通知客户端
            sendResponse(sessionId, VoiceResponse.state(VoiceResponse.StateData.builder()
                    .state(InterviewSessionState.INTERVIEWING.getCode())
                    .currentQuestion(state.getCurrentQuestion())
                    .totalQuestions(state.getTotalQuestions())
                    .assistRemaining(state.getAssistRemaining())
                    .elapsedTime(state.getElapsedTime())
                    .build()));
        }
    }

    public void endInterview(String sessionId) {
        SessionState state = sessionStates.get(sessionId);
        if (state != null) {
            state.setActive(false);
            state.setCompleted(true);
            log.info("[VoiceGateway] Interview ended, sessionId={}", sessionId);

            // 通知客户端
            sendResponse(sessionId, VoiceResponse.state(VoiceResponse.StateData.builder()
                    .state(InterviewSessionState.COMPLETED.getCode())
                    .currentQuestion(state.getCurrentQuestion())
                    .totalQuestions(state.getTotalQuestions())
                    .assistRemaining(state.getAssistRemaining())
                    .elapsedTime(state.getElapsedTime())
                    .build()));
        }
    }

    public VoiceResponse.StateData getSessionState(String sessionId) {
        SessionState state = sessionStates.get(sessionId);
        if (state == null) {
            return VoiceResponse.StateData.builder()
                    .state(InterviewSessionState.NOT_FOUND.getCode())
                    .build();
        }

        return VoiceResponse.StateData.builder()
                .state(state.getStateCode())
                .currentQuestion(state.getCurrentQuestion())
                .totalQuestions(state.getTotalQuestions())
                .assistRemaining(state.getAssistRemaining())
                .elapsedTime(state.getElapsedTime())
                .build();
    }

    public void sendResponse(String sessionId, VoiceResponse response) {
        WebSocketSession wsSession = wsSessions.get(sessionId);
        if (wsSession != null && wsSession.isOpen()) {
            voiceSessionManager.sendResponse(wsSession, response);
        }
    }

    public void broadcastResponse(VoiceResponse response) {
        voiceSessionManager.broadcastAll(response);
    }

    /**
     * 处理音频请求
     */
    private void handleAudioRequest(String sessionId, VoiceRequest<VoiceRequest.AudioData> request) {
        try {
            VoiceRequest.AudioData audioData = request.getData();
            String audioBase64 = audioData.getAudio();
            String format = audioData.getFormat() != null ? audioData.getFormat() : "pcm";
            Integer sampleRate = audioData.getSampleRate() != null ? audioData.getSampleRate() : 16000;

            byte[] audioDataBytes = Base64.getDecoder().decode(audioBase64);

            // 转发给音频处理器
            handleAudioData(sessionId, wsSessions.get(sessionId), audioDataBytes);

        } catch (Exception e) {
            log.error("[VoiceGateway] Failed to handle audio request, sessionId={}", sessionId, e);
        }
    }

    /**
     * 处理控制请求
     */
    private void handleControlRequest(String sessionId, VoiceRequest<VoiceRequest.ControlData> request) {
        try {
            VoiceRequest.ControlData controlData = request.getData();
            String actionCode = controlData.getAction();
            ControlAction action = ControlAction.fromCode(actionCode);

            if (action == null) {
                log.warn("[VoiceGateway] Unknown control action: {}", actionCode);
                return;
            }

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
                    log.warn("[VoiceGateway] Unhandled control action: {}", action);
            }
        } catch (Exception e) {
            log.error("[VoiceGateway] Failed to handle control request, sessionId={}", sessionId, e);
        }
    }

    /**
     * 处理开始动作
     */
    private void handleStartAction(String sessionId, VoiceRequest.ControlData controlData) {
        SessionState state = sessionStates.computeIfAbsent(sessionId, k -> new SessionState());

        // 设置初始状态
        state.setActive(true);
        state.setFrozen(false);
        state.setStartTime(System.currentTimeMillis());
        state.setPhase(InterviewPhaseEnum.WAITING_SELF_INTRODUCTION);

        // 处理扩展参数（如果有）
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

        log.info("[VoiceGateway] Interview started, sessionId={}", sessionId);

        // 通知客户端面试开始
        sendResponse(sessionId, VoiceResponse.state(VoiceResponse.StateData.builder()
                .state(InterviewSessionState.INTERVIEWING.getCode())
                .currentQuestion(state.getCurrentQuestion())
                .totalQuestions(state.getTotalQuestions())
                .assistRemaining(state.getAssistRemaining())
                .elapsedTime(0)
                .build()));

        // 请求自我介绍
        interviewerAgentHandler.requestSelfIntroduction(sessionId)
                .subscribe(
                        response -> sendResponse(sessionId, response),
                        error -> log.error("[VoiceGateway] 请求自我介绍失败, sessionId={}", sessionId, error),
                        () -> log.debug("[VoiceGateway] 自我介绍请求完成, sessionId={}", sessionId)
                );
    }

    /**
     * 处理停止动作（暂停录音）
     */
    private void handleStopAction(String sessionId) {
        SessionState state = sessionStates.get(sessionId);
        if (state != null) {
            log.info("[VoiceGateway] Recording stopped, sessionId={}", sessionId);
        }
    }

    /**
     * 获取会话状态对象（供内部使用）
     */
    public SessionState getInternalState(String sessionId) {
        return sessionStates.get(sessionId);
    }
}
