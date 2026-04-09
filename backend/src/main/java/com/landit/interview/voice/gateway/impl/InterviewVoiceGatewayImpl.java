package com.landit.interview.voice.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.landit.common.exception.BusinessException;
import com.landit.interview.entity.Interview;
import com.landit.interview.entity.InterviewSession;
import com.landit.interview.mapper.InterviewMapper;
import com.landit.interview.mapper.InterviewSessionMapper;
import com.landit.interview.voice.dto.*;
import com.landit.interview.voice.gateway.InterviewVoiceGateway;
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
 * 语音面试网关实现
 * 核心职责：会话状态管理、角色路由、录音存储
 * 使用 Spring 原生 WebSocket API
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewVoiceGatewayImpl implements InterviewVoiceGateway {

    private final InterviewerAgentHandler interviewerAgentHandler;
    private final AssistantAgentHandler assistantAgentHandler;
    private final QuestionPreGenerateService questionPreGenerateService;
    private final RecordingService recordingService;
    private final VoiceSessionManager voiceSessionManager;
    private final VoiceServiceFactory voiceServiceFactory;
    private final InterviewMapper interviewMapper;
    private final InterviewSessionMapper interviewSessionMapper;
    private final JobPositionMapper jobPositionMapper;
    private final ResumeMapper resumeMapper;

    // 会话状态：sessionId -> SessionState
    private final Map<String, SessionState> sessionStates = new ConcurrentHashMap<>();

    // WebSocket 会话：sessionId -> WebSocketSession
    private final Map<String, WebSocketSession> wsSessions = new ConcurrentHashMap<>();

    @Override
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

        // 8. 同步预生成开场白 + 第一个问题（阻塞等待完成后再返回响应）
        PreGenerateContext preGenContext = PreGenerateContext.builder()
                .position(jobPosition.getTitle())
                .jdContent(jobPosition.getJdContent())
                .resumeContent(resumeContent)
                .interviewerStyle(session.getInterviewerStyle())
                .totalQuestions(session.getTotalQuestions())
                .build();

        try {
            int generatedCount = questionPreGenerateService.preGenerateAllQuestions(sessionId, preGenContext);
            log.info("[VoiceGateway] 预生成完成, sessionId={}, count={}", sessionId, generatedCount);
        } catch (Exception ex) {
            log.error("[VoiceGateway] 预生成失败，将降级到实时生成, sessionId={}", sessionId, ex);
            // 预生成失败不阻断，面试开始时降级到实时生成
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

    @Override
    public void registerSession(String sessionId, WebSocketSession wsSession) {
        wsSessions.put(sessionId, wsSession);
        sessionStates.putIfAbsent(sessionId, new SessionState());
        log.info("[VoiceGateway] Session registered, sessionId={}", sessionId);
    }

    @Override
    public void unregisterSession(String sessionId, WebSocketSession wsSession) {
        wsSessions.remove(sessionId);
        SessionState state = sessionStates.get(sessionId);
        if (state != null) {
            state.setActive(false);
        }
        log.info("[VoiceGateway] Session unregistered, sessionId={}", sessionId);
    }

    @Override
    public void handleRequest(String sessionId, WebSocketSession wsSession, VoiceRequest request) {
        SessionState state = sessionStates.get(sessionId);
        if (state == null) {
            log.warn("[VoiceGateway] Session not found, sessionId={}", sessionId);
            return;
        }

        String type = request.getType();
        log.debug("[VoiceGateway] Handling request, sessionId={}, type={}", sessionId, type);

        switch (type) {
            case "audio":
                handleAudioRequest(sessionId, request);
                break;
            case "control":
                handleControlRequest(sessionId, request);
                break;
            default:
                log.warn("[VoiceGateway] Unknown request type: {}", type);
        }
    }

    @Override
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

    @Override
    public void handleSessionError(String sessionId, WebSocketSession wsSession, Throwable error) {
        log.error("[VoiceGateway] Session error, sessionId={}", sessionId, error);
        SessionState state = sessionStates.get(sessionId);
        if (state != null) {
            state.setActive(false);
        }
    }

    @Override
    public void freezeInterview(String sessionId) {
        SessionState state = sessionStates.get(sessionId);
        if (state != null) {
            state.setFrozen(true);
            state.setFreezeTime(System.currentTimeMillis());
            log.info("[VoiceGateway] Interview frozen, sessionId={}", sessionId);

            // 通知客户端
            sendResponse(sessionId, VoiceResponse.state(VoiceResponse.StateData.builder()
                    .state("frozen")
                    .currentQuestion(state.getCurrentQuestion())
                    .totalQuestions(state.getTotalQuestions())
                    .assistRemaining(state.getAssistRemaining())
                    .elapsedTime(state.getElapsedTime())
                    .build()));
        }
    }

    @Override
    public void resumeInterview(String sessionId) {
        SessionState state = sessionStates.get(sessionId);
        if (state != null) {
            state.setFrozen(false);
            log.info("[VoiceGateway] Interview resumed, sessionId={}", sessionId);

            // 通知客户端
            sendResponse(sessionId, VoiceResponse.state(VoiceResponse.StateData.builder()
                    .state("interviewing")
                    .currentQuestion(state.getCurrentQuestion())
                    .totalQuestions(state.getTotalQuestions())
                    .assistRemaining(state.getAssistRemaining())
                    .elapsedTime(state.getElapsedTime())
                    .build()));
        }
    }

    @Override
    public void endInterview(String sessionId) {
        SessionState state = sessionStates.get(sessionId);
        if (state != null) {
            state.setActive(false);
            state.setCompleted(true);
            log.info("[VoiceGateway] Interview ended, sessionId={}", sessionId);

            // 通知客户端
            sendResponse(sessionId, VoiceResponse.state(VoiceResponse.StateData.builder()
                    .state("completed")
                    .currentQuestion(state.getCurrentQuestion())
                    .totalQuestions(state.getTotalQuestions())
                    .assistRemaining(state.getAssistRemaining())
                    .elapsedTime(state.getElapsedTime())
                    .build()));
        }
    }

    @Override
    public VoiceResponse.StateData getSessionState(String sessionId) {
        SessionState state = sessionStates.get(sessionId);
        if (state == null) {
            return VoiceResponse.StateData.builder()
                    .state("not_found")
                    .build();
        }

        return VoiceResponse.StateData.builder()
                .state(state.getState())
                .currentQuestion(state.getCurrentQuestion())
                .totalQuestions(state.getTotalQuestions())
                .assistRemaining(state.getAssistRemaining())
                .elapsedTime(state.getElapsedTime())
                .build();
    }

    @Override
    public void sendResponse(String sessionId, VoiceResponse response) {
        WebSocketSession wsSession = wsSessions.get(sessionId);
        if (wsSession != null && wsSession.isOpen()) {
            voiceSessionManager.sendResponse(wsSession, response);
        }
    }

    @Override
    public void broadcastResponse(VoiceResponse response) {
        voiceSessionManager.broadcastAll(response);
    }

    /**
     * 处理音频请求
     */
    private void handleAudioRequest(String sessionId, VoiceRequest request) {
        try {
            Map<String, Object> data = (Map<String, Object>) request.getData();
            String audioBase64 = (String) data.get("audio");
            String format = (String) data.getOrDefault("format", "pcm");
            Integer sampleRate = (Integer) data.getOrDefault("sampleRate", 16000);

            byte[] audioData = Base64.getDecoder().decode(audioBase64);

            // 转发给音频处理器
            handleAudioData(sessionId, wsSessions.get(sessionId), audioData);

        } catch (Exception e) {
            log.error("[VoiceGateway] Failed to handle audio request, sessionId={}", sessionId, e);
        }
    }

    /**
     * 处理控制请求
     */
    private void handleControlRequest(String sessionId, VoiceRequest request) {
        try {
            Map<String, Object> data = (Map<String, Object>) request.getData();
            String action = (String) data.get("action");

            switch (action) {
                case "start":
                    handleStartAction(sessionId, data);
                    break;
                case "stop":
                    handleStopAction(sessionId);
                    break;
                case "end":
                    endInterview(sessionId);
                    break;
                case "freeze":
                    freezeInterview(sessionId);
                    break;
                case "resume":
                    resumeInterview(sessionId);
                    break;
                default:
                    log.warn("[VoiceGateway] Unknown control action: {}", action);
            }
        } catch (Exception e) {
            log.error("[VoiceGateway] Failed to handle control request, sessionId={}", sessionId, e);
        }
    }

    /**
     * 处理开始动作
     */
    private void handleStartAction(String sessionId, Map<String, Object> data) {
        SessionState state = sessionStates.computeIfAbsent(sessionId, k -> new SessionState());
        state.setActive(true);
        state.setFrozen(false);
        state.setStartTime(System.currentTimeMillis());

        if (data.containsKey("totalQuestions")) {
            state.setTotalQuestions((Integer) data.get("totalQuestions"));
        }
        if (data.containsKey("assistLimit")) {
            state.setAssistRemaining((Integer) data.get("assistLimit"));
        }

        log.info("[VoiceGateway] Interview started, sessionId={}", sessionId);

        // 通知客户端面试开始
        sendResponse(sessionId, VoiceResponse.state(VoiceResponse.StateData.builder()
                .state("interviewing")
                .currentQuestion(state.getCurrentQuestion())
                .totalQuestions(state.getTotalQuestions())
                .assistRemaining(state.getAssistRemaining())
                .elapsedTime(0)
                .build()));

        // 优先使用缓存的预生成问题（零延迟）
        Optional<PreGeneratedQuestion> cachedQuestion = questionPreGenerateService
                .getCachedQuestion(sessionId, 0);

        if (cachedQuestion.isPresent()) {
            PreGeneratedQuestion question = cachedQuestion.get();
            questionPreGenerateService.markQuestionUsed(sessionId, 0);

            // 零延迟推送预生成的开场白文本
            sendCachedQuestion(sessionId, question);

            // 全语音模式下，实时合成音频并推送
            if ("full_voice".equals(state.getVoiceMode())) {
                synthesizeAndSendAudio(sessionId, question.getText());
            }

            log.info("[VoiceGateway] 使用预生成开场白（零延迟）, sessionId={}", sessionId);

        } else {
            // 降级：实时生成
            log.warn("[VoiceGateway] 预生成未完成，降级到实时生成, sessionId={}", sessionId);
            interviewerAgentHandler.generateNextQuestion(sessionId)
                    .subscribe(
                            response -> sendResponse(sessionId, response),
                            error -> log.error("[VoiceGateway] 面试官生成开场白失败, sessionId={}", sessionId, error),
                            () -> log.debug("[VoiceGateway] 面试官开场白发送完成, sessionId={}", sessionId)
                    );
        }
    }

    /**
     * 推送缓存的问题（零延迟）
     * 只推送文本，音频由 handleStartAction 根据 voiceMode 决定是否实时合成
     */
    private void sendCachedQuestion(String sessionId, PreGeneratedQuestion question) {
        sendResponse(sessionId, VoiceResponse.transcript(VoiceResponse.TranscriptData.builder()
                .text(question.getText())
                .isFinal(true)
                .role("interviewer")
                .build()));
    }

    /**
     * 实时合成音频并推送（用于 full_voice 模式）
     */
    private void synthesizeAndSendAudio(String sessionId, String text) {
        try {
            TTSService ttsService = voiceServiceFactory.getTTSService();
            TTSConfig ttsConfig = TTSConfig.builder()
                    .model("cosyvoice-v1")
                    .voice("zhichu-v1")
                    .format("wav")
                    .sampleRate(16000)
                    .speechRate(1.0)
                    .volume(0.8)
                    .pitch(0.0)
                    .build();

            // 同步合成音频
            byte[] audioData = ttsService.streamSynthesize(text, ttsConfig)
                    .collectList()
                    .block()
                    .stream()
                    .reduce((a, b) -> {
                        byte[] result = new byte[a.length + b.length];
                        System.arraycopy(a, 0, result, 0, a.length);
                        System.arraycopy(b, 0, result, a.length, b.length);
                        return result;
                    })
                    .orElse(new byte[0]);

            if (audioData.length > 0) {
                String audioBase64 = Base64.getEncoder().encodeToString(audioData);
                sendResponse(sessionId, VoiceResponse.audio(VoiceResponse.AudioData.builder()
                        .audio(audioBase64)
                        .format("wav")
                        .sampleRate(16000)
                        .build()));
                log.debug("[VoiceGateway] TTS 音频推送完成, sessionId={}, audioSize={}", sessionId, audioData.length);
            }
        } catch (Exception e) {
            log.error("[VoiceGateway] TTS 合成失败, sessionId={}", sessionId, e);
        }
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

    /**
     * 会话状态内部类
     */
    @lombok.Data
    public static class SessionState {
        // 关联的真实面试信息
        private String interviewId;
        private String position;
        private String jdContent;
        private String resumeContent;
        // 面试官风格
        private String interviewerStyle = "professional";
        // 语音模式（half_voice/full_voice）
        private String voiceMode = "half_voice";
        // 会话状态
        private boolean active = false;
        private boolean frozen = false;
        private boolean completed = false;
        private long startTime;
        private long freezeTime;
        private int currentQuestion = 0;
        private int totalQuestions = 10;
        private int assistRemaining = 5;
        private int assistLimit = 5;
        private int segmentIndex = 0;

        public String getState() {
            if (completed) return "completed";
            if (frozen) return "frozen";
            if (active) return "interviewing";
            return "idle";
        }

        public int getElapsedTime() {
            if (startTime == 0) return 0;
            return (int) ((System.currentTimeMillis() - startTime) / 1000);
        }
    }
}
