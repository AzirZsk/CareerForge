package com.landit.interview.voice.controller;

import com.landit.interview.voice.dto.AssistSSEEvent;
import com.landit.interview.voice.service.StreamAssistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * AI 助手控制器
 * 处理语音面试中的快捷求助功能
 *
 * @author Azir
 */
@Slf4j
@RestController
@RequestMapping("/landit/interviews/sessions")
@RequiredArgsConstructor
public class AssistantController {

    private final StreamAssistService streamAssistService;

    /**
     * SSE 流式求助接口
     * 支持快捷求助（给我思路/解释概念/帮我润色）和自由提问
     *
     * @param sessionId      面试会话 ID
     * @param type           求助类型：GIVE_HINTS, EXPLAIN_CONCEPT, POLISH_ANSWER, FREE_QUESTION
     * @param question       自由提问内容（FREE_QUESTION 时必填）
     * @param candidateDraft 候选人草稿（POLISH_ANSWER 时使用）
     * @return SSE 事件流
     */
    @GetMapping(value = "/{sessionId}/assist/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AssistSSEEvent>> streamAssist(
            @PathVariable String sessionId,
            @RequestParam String type,
            @RequestParam(required = false) String question,
            @RequestParam(required = false) String candidateDraft) {

        log.info("[Assistant] Stream assist request, sessionId={}, type={}, hasQuestion={}",
                sessionId, type, question != null && !question.isEmpty());

        return streamAssistService.streamAssist(sessionId, type, question, candidateDraft)
                .map(event -> ServerSentEvent.<AssistSSEEvent>builder()
                        .event(event.getType())
                        .data(event)
                        .build())
                .doOnSubscribe(s -> log.info("[Assistant] SSE stream started, sessionId={}", sessionId))
                .doOnComplete(() -> log.info("[Assistant] SSE stream completed, sessionId={}", sessionId))
                .doOnError(e -> log.error("[Assistant] SSE stream error, sessionId={}", sessionId, e))
                .onErrorResume(e -> {
                    log.error("[Assistant] Returning error event, sessionId={}", sessionId, e);
                    return Flux.just(ServerSentEvent.<AssistSSEEvent>builder()
                            .event("error")
                            .data(AssistSSEEvent.error(AssistSSEEvent.ErrorEventData.builder()
                                    .code("ASSIST_ERROR")
                                    .message(e.getMessage())
                                    .build()))
                            .build());
                });
    }

    /**
     * 获取求助剩余次数
     *
     * @param sessionId 面试会话 ID
     * @return 剩余次数信息
     */
    @GetMapping("/{sessionId}/assist/remaining")
    public AssistRemainingResponse getAssistRemaining(@PathVariable String sessionId) {
        int remaining = streamAssistService.getAssistRemaining(sessionId);
        int limit = streamAssistService.getAssistLimit(sessionId);
        return new AssistRemainingResponse(remaining, limit);
    }

    /**
     * 求助剩余次数响应
     */
    public record AssistRemainingResponse(int remaining, int limit) {}
}
