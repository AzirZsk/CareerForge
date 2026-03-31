package com.landit.interview.voice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.interview.voice.dto.VoiceRequest;
import com.landit.interview.voice.dto.VoiceResponse;
import com.landit.interview.voice.gateway.InterviewVoiceGateway;
import com.landit.interview.voice.service.VoiceSessionManager;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;

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
@RequiredArgsConstructor
public class InterviewVoiceController {
    private static InterviewVoiceGateway voiceGateway;
    private static ObjectMapper objectMapper;
    private static VoiceSessionManager sessionManager;

    /**
     * 通过静态注入方式获取 Spring Bean
     * 由 WebSocketConfig 调用
     */
    public static void setBeans(InterviewVoiceGateway gateway, ObjectMapper mapper, VoiceSessionManager manager) {
        voiceGateway = gateway;
        objectMapper = mapper;
        sessionManager = manager;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("sessionId") String sessionId) {
        log.info("[VoiceWS] Connection opened, sessionId={}, wsSessionId={}", sessionId, session.getId());
        // 注册会话到 SessionManager
        sessionManager.registerSession(session.getId(), session);
        try {
            if (voiceGateway != null) {
                voiceGateway.registerSession(sessionId, session);
            }
            // 发送连接成功消息
            sessionManager.sendResponse(session, VoiceResponse.state(VoiceResponse.StateData.builder()
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
            sessionManager.sendError(session, "PARSE_ERROR", "Failed to parse message: " + e.getMessage());
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
        // 从 SessionManager 注销会话
        sessionManager.unregisterSession(session.getId());
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
        // 从 SessionManager 注销会话
        sessionManager.unregisterSession(session.getId());
        try {
            if (voiceGateway != null) {
                voiceGateway.handleSessionError(sessionId, session, error);
            }
        } catch (Exception e) {
            log.error("[VoiceWS] Failed to handle error, sessionId={}", sessionId, e);
        }
    }
}
