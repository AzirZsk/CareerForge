package com.landit.interview.voice.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.interview.voice.dto.VoiceRequest;
import com.landit.interview.voice.dto.VoiceResponse;
import com.landit.interview.voice.enums.MessageType;
import com.landit.interview.voice.gateway.InterviewVoiceGateway;
import com.landit.interview.voice.service.VoiceSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

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
        }
    }

    private void handleTextMessage(WebSocketSession session, TextMessage message, String sessionId) {
        log.debug("[VoiceWS] Received text message, sessionId={}, length={}", sessionId, message.getPayload().length());
        try {
            String payload = message.getPayload();

            // 先解析为 JsonNode 判断消息类型
            JsonNode jsonNode = objectMapper.readTree(payload);
            MessageType messageType = MessageType.fromCode(jsonNode.path("type").asText());

            // 处理心跳消息
            if (messageType == MessageType.PING) {
                sessionManager.sendResponse(session, VoiceResponse.pong());
                return;
            }

            // 根据消息类型选择对应的泛型类型进行反序列化
            switch (messageType) {
                case AUDIO:
                    VoiceRequest<VoiceRequest.AudioData> audioRequest = objectMapper.treeToValue(
                            jsonNode,
                            new TypeReference<VoiceRequest<VoiceRequest.AudioData>>() {}
                    );
                    voiceGateway.handleRequest(sessionId, session, audioRequest);
                    break;
                case CONTROL:
                    VoiceRequest<VoiceRequest.ControlData> controlRequest = objectMapper.treeToValue(
                            jsonNode,
                            new TypeReference<VoiceRequest<VoiceRequest.ControlData>>() {}
                    );
                    voiceGateway.handleRequest(sessionId, session, controlRequest);
                    break;
                default:
                    log.warn("[VoiceWS] Unknown message type: {}", messageType);
                    sessionManager.sendError(session, "UNKNOWN_TYPE", "Unknown message type: " + messageType);
            }
        } catch (Exception e) {
            log.error("[VoiceWS] Failed to handle text message, sessionId={}", sessionId, e);
            sessionManager.sendError(session, "PARSE_ERROR", "Failed to parse message: " + e.getMessage());
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
