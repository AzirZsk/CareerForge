package com.careerforge.interview.voice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.careerforge.interview.voice.dto.VoiceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * WebSocket 会话管理器
 * 负责管理 WebSocket 会话的生命周期和消息发送
 * 使用 Spring 原生 WebSocket API
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceSessionManager {
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    // 每个 session 一把锁，防止多线程并发写入 WebSocket 状态机冲突
    private final ConcurrentHashMap<String, ReentrantLock> sessionLocks = new ConcurrentHashMap<>();

    /**
     * 注册 WebSocket 会话
     *
     * @param wsSessionId WebSocket 会话 ID
     * @param session     WebSocket 会话
     */
    public void registerSession(String wsSessionId, WebSocketSession session) {
        sessions.put(wsSessionId, session);
        log.debug("[VoiceSessionManager] 会话已注册, wsSessionId={}", wsSessionId);
    }

    /**
     * 注销 WebSocket 会话
     *
     * @param wsSessionId WebSocket 会话 ID
     */
    public void unregisterSession(String wsSessionId) {
        sessions.remove(wsSessionId);
        sessionLocks.remove(wsSessionId);
        log.debug("[VoiceSessionManager] 会话已注销, wsSessionId={}", wsSessionId);
    }

    /**
     * 获取 WebSocket 会话
     *
     * @param wsSessionId WebSocket 会话 ID
     * @return WebSocket 会话，如果不存在则返回 null
     */
    public WebSocketSession getSession(String wsSessionId) {
        return sessions.get(wsSessionId);
    }

    /**
     * 发送响应消息给指定会话
     *
     * @param session  WebSocket 会话
     * @param response 响应数据
     */
    public void sendResponse(WebSocketSession session, VoiceResponse response) {
        if (session == null || !session.isOpen()) {
            log.warn("[VoiceSessionManager] 会话为空或已关闭，无法发送响应");
            return;
        }
        String sessionId = session.getId();
        ReentrantLock lock = sessionLocks.computeIfAbsent(sessionId, k -> new ReentrantLock());
        lock.lock();
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(response);
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            log.error("[VoiceSessionManager] 发送响应失败", e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 发送响应消息给指定 WebSocket 会话 ID
     *
     * @param wsSessionId WebSocket 会话 ID
     * @param response    响应数据
     */
    public void sendResponse(String wsSessionId, VoiceResponse response) {
        WebSocketSession session = sessions.get(wsSessionId);
        sendResponse(session, response);
    }

    /**
     * 发送错误消息
     *
     * @param session  WebSocket 会话
     * @param code     错误码
     * @param message  错误消息
     */
    public void sendError(WebSocketSession session, String code, String message) {
        sendResponse(session, VoiceResponse.error(VoiceResponse.ErrorData.builder()
                .code(code)
                .message(message)
                .build()));
    }

    /**
     * 检查会话是否存在且打开
     *
     * @param wsSessionId WebSocket 会话 ID
     * @return true 表示会话存在且打开
     */
    public boolean isSessionOpen(String wsSessionId) {
        WebSocketSession session = sessions.get(wsSessionId);
        return session != null && session.isOpen();
    }
}
