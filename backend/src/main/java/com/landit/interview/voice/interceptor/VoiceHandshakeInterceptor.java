package com.landit.interview.voice.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * 语音面试 WebSocket 握手拦截器
 * 用于从 URL 路径中提取 sessionId 参数
 *
 * @author Azir
 */
@Slf4j
@Component
public class VoiceHandshakeInterceptor implements HandshakeInterceptor {

    private static final String WS_PATH_PREFIX = "/landit/ws/interview/voice/";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String path = request.getURI().getPath();
        log.info("[VoiceHandshake] 握手请求, path={}", path);
        // 从路径提取 sessionId: /landit/ws/interview/voice/{sessionId}
        String sessionId = extractSessionId(path);
        if (sessionId != null && !sessionId.isEmpty()) {
            attributes.put("sessionId", sessionId);
            log.info("[VoiceHandshake] 提取sessionId={}", sessionId);
            return true;
        }
        log.warn("[VoiceHandshake] 无法从路径提取sessionId, path={}", path);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("[VoiceHandshake] 握手失败", exception);
        }
    }

    /**
     * 从路径中提取 sessionId
     * 路径格式: /landit/ws/interview/voice/{sessionId}
     */
    private String extractSessionId(String path) {
        if (path == null || !path.startsWith(WS_PATH_PREFIX)) {
            return null;
        }
        String sessionId = path.substring(WS_PATH_PREFIX.length());
        // 移除可能的查询参数
        int queryIndex = sessionId.indexOf('?');
        if (queryIndex > 0) {
            sessionId = sessionId.substring(0, queryIndex);
        }
        return sessionId;
    }
}
