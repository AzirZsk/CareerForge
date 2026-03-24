package com.landit.chat.handler;

import com.landit.chat.dto.ChatEvent;
import com.landit.chat.dto.ChatStreamRequest;
import com.landit.chat.service.AIChatService;
import com.landit.resume.util.GraphSseHelper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * AI聊天处理器
 * 负责SSE流式输出管理
 * 复用ResumeOptimizeGraphHandler的SSE模式
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AIChatHandler {

    private final AIChatService chatService;

    /**
     * SSE超时时间（5分钟）
     */
    private static final long SSE_TIMEOUT = 300_000L;

    /**
     * 流式处理聊天请求
     *
     * @param request 聊天请求（包含图片字段）
     * @param response HTTP响应
     * @return SSE事件发射器
     */
    public SseEmitter streamChat(ChatStreamRequest request, HttpServletResponse response) {
        log.info("[AIChat] 开始SSE流式聊天: resumeId={}, hasImage={}",
                request.getResumeId(),
                request.getImage() != null && !request.getImage().isEmpty());

        GraphSseHelper.configureSseResponse(response);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        chatService.chat(request).subscribe(
                event -> sendSseEvent(emitter, event),
                error -> {
                    log.error("[AIChat] 流异常", error);
                    try {
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(ChatEvent.error(error.getMessage())));
                        emitter.complete();
                    } catch (IOException e) {
                        log.error("[AIChat] 发送错误事件失败", e);
                    }
                },
                () -> {
                    log.info("[AIChat] 流完成");
                    emitter.complete();
                }
        );

        // 设置超时回调
        emitter.onTimeout(() -> {
            log.warn("[AIChat] SSE连接超时");
            emitter.complete();
        });

        // 设置错误回调
        emitter.onError(e -> {
            log.error("[AIChat] SSE连接错误", e);
        });

        return emitter;
    }


    /**
     * 发送SSE事件
     */
    private void sendSseEvent(SseEmitter emitter, ChatEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(event));
            log.debug("[AIChat] 发送事件: type={}", event.getType());
        } catch (IOException e) {
            log.error("[AIChat] 发送失败", e);
            emitter.completeWithError(e);
        }
    }
}
