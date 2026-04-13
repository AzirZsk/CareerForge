package com.careerforge.interview.voice.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.careerforge.interview.voice.dto.VoiceRequest;
import com.careerforge.interview.voice.dto.VoiceResponse;
import com.careerforge.interview.voice.enums.MessageType;
import com.careerforge.interview.voice.gateway.InterviewVoiceGateway;
import com.careerforge.interview.voice.service.VoiceSessionManager;
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
        log.info("[VoiceWS] 连接建立, sessionId={}, wsSessionId={}", sessionId, session.getId());

        // 设置文本消息大小限制为 1MB（默认 64KB 太小，ASR 返回的文本可能很大）
        session.setTextMessageSizeLimit(1024 * 1024);

        // 注册会话
        sessionManager.registerSession(session.getId(), session);
        voiceGateway.registerSession(sessionId, session);

        // 预生成在 createSession 时已同步完成，直接通知前端准备就绪
        sessionManager.sendResponse(session, VoiceResponse.ready(VoiceResponse.ReadyData.builder()
                .state("ready")
                .message("面试问题已生成，准备就绪")
                .build()));
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String sessionId = getSessionId(session);
        if (message instanceof TextMessage) {
            handleTextMessage(session, (TextMessage) message, sessionId);
        }
    }

    /**
     * 处理文本消息（JSON 格式）
     * 解析消息类型后直接路由到 Gateway 对应的处理方法，避免泛型擦除导致的重复判断
     */
    private void handleTextMessage(WebSocketSession session, TextMessage message, String sessionId) {
        log.debug("[VoiceWS] 收到文本消息, sessionId={}, length={}", sessionId, message.getPayload().length());
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

            // 根据消息类型反序列化并直接路由到 Gateway 的具体方法（避免重复判断）
            switch (messageType) {
                case AUDIO:
                    VoiceRequest<VoiceRequest.AudioData> audioRequest = objectMapper.treeToValue(
                            jsonNode,
                            new TypeReference<VoiceRequest<VoiceRequest.AudioData>>() {}
                    );
                    voiceGateway.handleAudioRequest(sessionId, audioRequest);
                    break;
                case CONTROL:
                    VoiceRequest<VoiceRequest.ControlData> controlRequest = objectMapper.treeToValue(
                            jsonNode,
                            new TypeReference<VoiceRequest<VoiceRequest.ControlData>>() {}
                    );
                    voiceGateway.handleControlRequest(sessionId, controlRequest);
                    break;
                default:
                    log.warn("[VoiceWS] 未知消息类型: {}", messageType);
                    sessionManager.sendError(session, "UNKNOWN_TYPE", "Unknown message type: " + messageType);
            }
        } catch (Exception e) {
            log.error("[VoiceWS] 处理文本消息失败, sessionId={}", sessionId, e);
            sessionManager.sendError(session, "PARSE_ERROR", "Failed to parse message: " + e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = getSessionId(session);
        log.error("[VoiceWS] 传输错误, sessionId={}", sessionId, exception);
        voiceGateway.handleSessionError(sessionId, session, exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = getSessionId(session);
        log.info("[VoiceWS] 连接关闭, sessionId={}, status={}", sessionId, status);

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
