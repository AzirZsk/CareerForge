package com.careerforge.interview.voice.service;

import com.careerforge.interview.voice.dto.AssistSSEEvent;
import com.careerforge.interview.voice.dto.SessionState;
import com.careerforge.interview.voice.gateway.InterviewVoiceGateway;
import com.careerforge.interview.voice.handler.AssistantAgentHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * 流式求助服务
 * 处理面试过程中的快捷求助功能
 * 采用回调模式，事件通过 Consumer 直接推送到 SseEmitter
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

        // 扣减求助次数
        SessionState state = voiceGateway.getInternalState(sessionId);
        if (state != null) {
            state.setAssistRemaining(state.getAssistRemaining() - 1);
            log.info("[StreamAssist] 求助次数已扣减, sessionId={}, remaining={}", sessionId, state.getAssistRemaining());
        }

        // 调用助手处理器，事件通过回调直接推送
        assistantAgentHandler.handleAssist(sessionId, assistType, question, candidateDraft,
                event -> {
                    sendEvent(emitter, event);
                    // 检测终止事件
                    if ("done".equals(event.getType()) || "error".equals(event.getType())) {
                        voiceGateway.resumeInterview(sessionId);
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
        SessionState state = voiceGateway.getInternalState(sessionId);
        return state != null ? state.getAssistRemaining() : 0;
    }

    /**
     * 获取求助次数上限
     *
     * @param sessionId 会话 ID
     * @return 上限
     */
    public int getAssistLimit(String sessionId) {
        SessionState state = voiceGateway.getInternalState(sessionId);
        return state != null ? state.getAssistLimit() : DEFAULT_ASSIST_LIMIT;
    }

    /**
     * 检查是否可以求助
     *
     * @param sessionId 会话 ID
     * @return true 表示可以求助
     */
    public boolean canAssist(String sessionId) {
        return getAssistRemaining(sessionId) > 0;
    }
}
