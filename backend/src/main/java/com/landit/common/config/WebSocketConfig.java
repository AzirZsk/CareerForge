package com.landit.common.config;

import com.landit.interview.voice.handler.VoiceWebSocketHandler;
import com.landit.interview.voice.interceptor.VoiceHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置类
 * 使用 Spring 原生 WebSocket API
 *
 * @author Azir
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final VoiceWebSocketHandler voiceWebSocketHandler;
    private final VoiceHandshakeInterceptor voiceHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(voiceWebSocketHandler, "/ws/interview/voice/*")
                .addInterceptors(voiceHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
