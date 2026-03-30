package com.landit.interview.voice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.interview.voice.dto.VoiceRequest;
import com.landit.interview.voice.dto.VoiceResponse;
import com.landit.interview.voice.gateway.InterviewVoiceGateway;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 语音面试控制器
 * 处理实时语音对话
 *
 * <p>端点：/landit/ws/interview/voice/{sessionId}
 *
 * @author Azir
 */
@Slf4j
@Component
@ServerEndpoint("/ws/interview/voice/{sessionId}")
public class InterviewVoiceController {

    private static final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    private static InterviewVoiceGateway voiceGateway;
    private static ObjectMapper objectMapper;

    /**
     * 通过静态注入方式获取 Spring Bean
     */
    public static void setVoiceGateway(InterviewVoiceGateway gateway, ObjectMapper mapper) {
        voiceGateway = gateway;
        objectMapper = mapper;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("sessionId") String sessionId) {
        log.info("[VoiceWS] Connection opened, sessionId={}, wsSessionId={}", sessionId, session.getId());
        sessions.put(session.getId(), session);

        try {
            if (voiceGateway != null) {
                voiceGateway.registerSession(sessionId, session);
            }

            // 发送连接成功消息
            sendResponse(session, VoiceResponse.state(VoiceResponse.StateData.builder()
                    .state("connected")
                    .build()));
        } catch (Exception e) {
            log.error("[VoiceWS] Failed to handle open, sessionId={}", sessionId, e);
        }
    }

    @OnMessage
    public void onTextMessage(String message, Session session, @PathParam("sessionId") String sessionId) {
        log.debug("[VoiceWS] Received text message, sessionId={}, length={}", sessionId, message.length());

        try {
            VoiceRequest request = objectMapper.readValue(message, VoiceRequest.class);

            if (voiceGateway != null) {
                voiceGateway.handleRequest(sessionId, session, request);
            }
        } catch (Exception e) {
            log.error("[VoiceWS] Failed to handle text message, sessionId={}", sessionId, e);
            sendError(session, "PARSE_ERROR", "Failed to parse message: " + e.getMessage());
        }
    }

    @OnMessage
    public void onBinaryMessage(ByteBuffer buffer, Session session, @PathParam("sessionId") String sessionId) {
        log.trace("[VoiceWS] Received binary message, sessionId={}, size={}", sessionId, buffer.remaining());

        try {
            byte[] audioData = new byte[buffer.remaining()];
            buffer.get(audioData);

            if (voiceGateway != null) {
                voiceGateway.handleAudioData(sessionId, session, audioData);
            }
        } catch (Exception e) {
            log.error("[VoiceWS] Failed to handle binary message, sessionId={}", sessionId, e);
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("sessionId") String sessionId, CloseReason reason) {
        log.info("[VoiceWS] Connection closed, sessionId={}, reason={}", sessionId, reason);
        sessions.remove(session.getId());

        try {
            if (voiceGateway != null) {
                voiceGateway.unregisterSession(sessionId, session);
            }
        } catch (Exception e) {
            log.error("[VoiceWS] Failed to handle close, sessionId={}", sessionId, e);
        }
    }

    @OnError
    public void onError(Session session, @PathParam("sessionId") String sessionId, Throwable error) {
        log.error("[VoiceWS] Connection error, sessionId={}", sessionId, error);
        sessions.remove(session.getId());

        try {
            if (voiceGateway != null) {
                voiceGateway.handleSessionError(sessionId, session, error);
            }
        } catch (Exception e) {
            log.error("[VoiceWS] Failed to handle error, sessionId={}", sessionId, e);
        }
    }

    /**
     * 发送响应消息
     */
    public static void sendResponse(Session session, VoiceResponse response) {
        if (session == null || !session.isOpen()) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(response);
            session.getBasicRemote().sendText(json);
        } catch (IOException e) {
            log.error("[VoiceWS] Failed to send response", e);
        }
    }

    /**
     * 发送错误消息
     */
    public static void sendError(Session session, String code, String message) {
        sendResponse(session, VoiceResponse.error(VoiceResponse.ErrorData.builder()
                .code(code)
                .message(message)
                .build()));
    }

    /**
     * 广播消息给指定会话的所有连接
     */
    public static void broadcastToSession(String sessionId, VoiceResponse response) {
        sessions.values().stream()
                .filter(Session::isOpen)
                .forEach(session -> sendResponse(session, response));
    }
}
