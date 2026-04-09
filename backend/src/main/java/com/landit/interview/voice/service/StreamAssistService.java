package com.landit.interview.voice.service;

import com.landit.interview.voice.dto.AssistSSEEvent;
import com.landit.interview.voice.gateway.InterviewVoiceGateway;
import com.landit.interview.voice.handler.AssistantAgentHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * 流式求助服务
 * 处理面试过程中的快捷求助功能
 *
 * @author Azir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreamAssistService {

    private final AssistantAgentHandler assistantAgentHandler;
    private final InterviewVoiceGateway voiceGateway;

    // 默认求助次数上限
    private static final int DEFAULT_ASSIST_LIMIT = 5;

    /**
     * 流式求助（使用 SseEmitter）
     *
     * @param sessionId      会话 ID
     * @param assistType     求助类型
     * @param question       自由提问内容
     * @param candidateDraft 候选人草稿
     * @param emitter        SSE 发射器
     */
    public void streamAssist(
            String sessionId,
            String assistType,
            String question,
            String candidateDraft,
            SseEmitter emitter) {

        log.info("[StreamAssist] 处理求助请求, sessionId={}, type={}", sessionId, assistType);

        // 检查是否可以求助
        if (!canAssist(sessionId)) {
            log.warn("[StreamAssist] 求助次数已用尽, sessionId={}", sessionId);
            sendEvent(emitter, AssistSSEEvent.error(
                    AssistSSEEvent.ErrorEventData.builder()
                            .code("ASSIST_LIMIT_EXCEEDED")
                            .message("求助次数已用完")
                            .build()
            ));
            emitter.complete();
            return;
        }

        // 冻结面试
        voiceGateway.freezeInterview(sessionId);

        // 异步执行求助逻辑
        CompletableFuture.runAsync(() -> {
            try {
                // 调用助手处理器获取 Flux
                assistantAgentHandler.handleAssist(sessionId, assistType, question, candidateDraft)
                        .subscribe(
                                event -> {
                                    // 发送每个事件
                                    sendEvent(emitter, event);
                                },
                                error -> {
                                    // 出错处理
                                    log.error("[StreamAssist] 求助出错, sessionId={}", sessionId, error);
                                    voiceGateway.resumeInterview(sessionId);
                                    sendEvent(emitter, AssistSSEEvent.error(
                                            AssistSSEEvent.ErrorEventData.builder()
                                                    .code("ASSIST_ERROR")
                                                    .message(error.getMessage())
                                                    .build()
                                    ));
                                    emitter.complete();
                                },
                                () -> {
                                    // 完成处理
                                    log.info("[StreamAssist] 求助完成, sessionId={}", sessionId);
                                    emitter.complete();
                                }
                        );
            } catch (Exception e) {
                log.error("[StreamAssist] 意外错误, sessionId={}", sessionId, e);
                voiceGateway.resumeInterview(sessionId);
                sendEvent(emitter, AssistSSEEvent.error(
                        AssistSSEEvent.ErrorEventData.builder()
                                .code("ASSIST_ERROR")
                                .message(e.getMessage())
                                .build()
                ));
                emitter.complete();
            }
        });
    }

    /**
     * 发送 SSE 事件
     */
    private void sendEvent(SseEmitter emitter, AssistSSEEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .name(event.getType())
                    .data(event));
        } catch (IOException e) {
            log.error("[StreamAssist] 发送事件失败", e);
            emitter.completeWithError(e);
        }
    }

    /**
     * 获取剩余求助次数
     *
     * @param sessionId 会话 ID
     * @return 剩余次数
     */
    public int getAssistRemaining(String sessionId) {
        var state = voiceGateway.getSessionState(sessionId);
        return state.getAssistRemaining();
    }

    /**
     * 获取求助次数上限
     *
     * @param sessionId 会话 ID
     * @return 上限
     */
    public int getAssistLimit(String sessionId) {
        var state = voiceGateway.getSessionState(sessionId);
        // 如果状态中没有设置，返回默认值
        return state.getTotalQuestions() > 0 ? DEFAULT_ASSIST_LIMIT : DEFAULT_ASSIST_LIMIT;
    }

    /**
     * 检查是否可以求助
     *
     * @param sessionId 会话 ID
     * @return true 表示可以求助
     */
    public boolean canAssist(String sessionId) {
        int remaining = getAssistRemaining(sessionId);
        return remaining > 0;
    }
}
