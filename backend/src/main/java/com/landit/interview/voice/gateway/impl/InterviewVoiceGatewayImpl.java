package com.landit.interview.voice.gateway.impl;

import com.landit.interview.voice.dto.*;
import com.landit.interview.voice.gateway.InterviewVoiceGateway;
import com.landit.interview.voice.handler.AssistantAgentHandler;
import com.landit.interview.voice.handler.InterviewerAgentHandler;
import com.landit.interview.voice.service.RecordingService;
import com.landit.interview.voice.service.VoiceSessionManager;
import jakarta.websocket.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语音面试网关实现
 * 核心职责：会话状态管理、角色路由、录音存储
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewVoiceGatewayImpl implements InterviewVoiceGateway {

    private final InterviewerAgentHandler interviewerAgentHandler;
    private final AssistantAgentHandler assistantAgentHandler;
    private final RecordingService recordingService;
    private final VoiceSessionManager voiceSessionManager;

    // 会话状态：sessionId -> SessionState
    private final Map<String, SessionState> sessionStates = new ConcurrentHashMap<>();

    // WebSocket 会话：sessionId -> Session
    private final Map<String, Session> wsSessions = new ConcurrentHashMap<>();

    @Override
    public void registerSession(String sessionId, Session wsSession) {
        wsSessions.put(sessionId, wsSession);
        sessionStates.putIfAbsent(sessionId, new SessionState());
        log.info("[VoiceGateway] Session registered, sessionId={}", sessionId);
    }

    @Override
    public void unregisterSession(String sessionId, Session wsSession) {
        wsSessions.remove(sessionId);
        SessionState state = sessionStates.get(sessionId);
        if (state != null) {
            state.setActive(false);
        }
        log.info("[VoiceGateway] Session unregistered, sessionId={}", sessionId);
    }

    @Override
    public void handleRequest(String sessionId, Session wsSession, VoiceRequest request) {
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
    public void handleAudioData(String sessionId, Session wsSession, byte[] audioData) {
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
    public void handleSessionError(String sessionId, Session wsSession, Throwable error) {
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
        Session wsSession = wsSessions.get(sessionId);
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

        // 通知客户端
        sendResponse(sessionId, VoiceResponse.state(VoiceResponse.StateData.builder()
                .state("interviewing")
                .currentQuestion(state.getCurrentQuestion())
                .totalQuestions(state.getTotalQuestions())
                .assistRemaining(state.getAssistRemaining())
                .elapsedTime(0)
                .build()));
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
