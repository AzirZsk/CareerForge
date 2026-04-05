package com.landit.interview.voice.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.interview.voice.dto.VoiceRequest;
import com.landit.interview.voice.dto.VoiceResponse;
import com.landit.interview.voice.gateway.InterviewVoiceGateway;
import com.landit.interview.voice.service.VoiceSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.nio.ByteBuffer;

/**
 * 语音面试 WebSocket 处理器
 * 使用 Spring 原生 WebSocket API
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceWebSocketHandler implements WebSocketHandler {

    private final InterviewVoiceGateway voiceGateway;
    private final VoiceSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = getSessionId(session);
        log.info("[VoiceWS] Connection established, sessionId={}, wsSessionId={}", sessionId, session.getId());

        // 设置文本消息大小限制为 1MB（默认 64KB 太小，ASR 返回的文本可能很大）
        session.setTextMessageSizeLimit(1024 * 1024);
        // 设置二进制消息大小限制为 10MB
        session.setBinaryMessageSizeLimit(10 * 1024 * 1024);

        // 注册会话
        sessionManager.registerSession(session.getId(), session);
        voiceGateway.registerSession(sessionId, session);

        // 发送连接成功消息
        sessionManager.sendResponse(session, VoiceResponse.state(VoiceResponse.StateData.builder()
                .state("connected")
                .build()));
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String sessionId = getSessionId(session);

        if (message instanceof TextMessage) {
            handleTextMessage(session, (TextMessage) message, sessionId);
        } else if (message instanceof BinaryMessage) {
            handleBinaryMessage(session, (BinaryMessage) message, sessionId);
        }
    }

    private void handleTextMessage(WebSocketSession session, TextMessage message, String sessionId) {
        log.debug("[VoiceWS] Received text message, sessionId={}, length={}", sessionId, message.getPayload().length());
        try {
            VoiceRequest request = objectMapper.readValue(message.getPayload(), VoiceRequest.class);
            voiceGateway.handleRequest(sessionId, session, request);
        } catch (Exception e) {
            log.error("[VoiceWS] Failed to handle text message, sessionId={}", sessionId, e);
            sessionManager.sendError(session, "PARSE_ERROR", "Failed to parse message: " + e.getMessage());
        }
    }

    private void handleBinaryMessage(WebSocketSession session, BinaryMessage message, String sessionId) {
        log.trace("[VoiceWS] Received binary message, sessionId={}, size={}", sessionId, message.getPayload().array().length);
        try {
            ByteBuffer buffer = message.getPayload();
            byte[] audioData = new byte[buffer.remaining()];
            buffer.get(audioData);
            voiceGateway.handleAudioData(sessionId, session, audioData);
        } catch (Exception e) {
            log.error("[VoiceWS] Failed to handle binary message, sessionId={}", sessionId, e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = getSessionId(session);
        log.error("[VoiceWS] Transport error, sessionId={}", sessionId, exception);
        voiceGateway.handleSessionError(sessionId, session, exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = getSessionId(session);
        log.info("[VoiceWS] Connection closed, sessionId={}, status={}", sessionId, status);

        sessionManager.unregisterSession(session.getId());
        voiceGateway.unregisterSession(sessionId, session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 从 session attributes 中获取 sessionId
     */
    private String getSessionId(WebSocketSession session) {
        return (String) session.getAttributes().get("sessionId");
    }
}
