package com.careerforge.interview.voice.controller;

import com.careerforge.common.response.ApiResponse;
import com.careerforge.interview.voice.dto.VoiceSessionCreateRequest;
import com.careerforge.interview.voice.dto.VoiceSessionCreateVO;
import com.careerforge.interview.voice.gateway.InterviewVoiceGateway;
import com.careerforge.interview.voice.service.StreamAssistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 助手控制器
 * 处理语音面试中的快捷求助功能
 *
 * @author Azir
 */
@Slf4j
@RestController
@RequestMapping("/interviews/sessions")
@RequiredArgsConstructor
public class AssistantController {

    private final StreamAssistService streamAssistService;
    private final InterviewVoiceGateway voiceGateway;

    // SSE 超时时间（10分钟）
    private static final long SSE_TIMEOUT = 600000L;

    /**
     * 创建语音面试会话
     * 从真实面试详情页进入，关联 Interview
     *
     * @param request 创建请求
     * @return 会话信息
     */
    @PostMapping
    public ApiResponse<VoiceSessionCreateVO> createSession(@Valid @RequestBody VoiceSessionCreateRequest request) {
        log.info("[VoiceSession] 创建会话请求, interviewId={}", request.getInterviewId());
        VoiceSessionCreateVO vo = voiceGateway.createSession(request);
        return ApiResponse.success(vo);
    }

    /**
     * SSE 流式求助接口
     * 支持快捷求助（给我思路/解释概念/帮我润色）和自由提问
     *
     * @param sessionId      面试会话 ID
     * @param type           求助类型：give_hints, explain_concept, polish_answer, free_question
     * @param question       自由提问内容（free_question 时必填）
     * @param candidateDraft 候选人草稿（polish_answer 时使用）
     * @return SSE 发射器
     */
    @GetMapping(value = "/{sessionId}/assist/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAssist(
            @PathVariable String sessionId,
            @RequestParam String type,
            @RequestParam(required = false) String question,
            @RequestParam(required = false) String candidateDraft) {

        log.info("[Assistant] 流式求助请求, sessionId={}, type={}, hasQuestion={}",
                sessionId, type, question != null && !question.isEmpty());

        // 创建 SSE 发射器
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 设置超时和错误处理
        emitter.onTimeout(() -> {
            log.warn("[Assistant] SSE超时, sessionId={}", sessionId);
            emitter.complete();
        });

        emitter.onError(e -> {
            log.error("[Assistant] SSE错误, sessionId={}", sessionId, e);
            emitter.completeWithError(e);
        });

        // 调用 Service 处理流式求助
        streamAssistService.streamAssist(sessionId, type, question, candidateDraft, emitter);

        return emitter;
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
