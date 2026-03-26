package com.landit.interview.voice.service.impl;

import com.landit.interview.voice.dto.AssistSSEEvent;
import com.landit.interview.voice.gateway.InterviewVoiceGateway;
import com.landit.interview.voice.handler.AssistantAgentHandler;
import com.landit.interview.voice.service.StreamAssistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 流式求助服务实现
 * 处理面试过程中的快捷求助功能
 *
 * @author Azir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreamAssistServiceImpl implements StreamAssistService {

    private final AssistantAgentHandler assistantAgentHandler;
    private final InterviewVoiceGateway voiceGateway;

    // 默认求助次数上限
    private static final int DEFAULT_ASSIST_LIMIT = 5;

    @Override
    public Flux<AssistSSEEvent> streamAssist(
            String sessionId,
            String assistType,
            String question,
            String candidateDraft) {

        log.info("[StreamAssist] Processing assist request, sessionId={}, type={}", sessionId, assistType);

        // 检查是否可以求助
        if (!canAssist(sessionId)) {
            log.warn("[StreamAssist] Assist limit exceeded, sessionId={}", sessionId);
            return Flux.just(AssistSSEEvent.error(
                    AssistSSEEvent.ErrorEventData.builder()
                            .code("ASSIST_LIMIT_EXCEEDED")
                            .message("求助次数已用完")
                            .build()
            ));
        }

        // 冻结面试
        voiceGateway.freezeInterview(sessionId);

        // 调用助手处理器
        return assistantAgentHandler.handleAssist(sessionId, assistType, question, candidateDraft)
                .doOnComplete(() -> {
                    log.info("[StreamAssist] Assist completed, sessionId={}", sessionId);
                    // 注意：不在这里恢复面试，让用户手动点击返回
                })
                .doOnError(error -> {
                    log.error("[StreamAssist] Assist error, sessionId={}", sessionId, error);
                    // 出错时自动恢复面试
                    voiceGateway.resumeInterview(sessionId);
                });
    }

    @Override
    public int getAssistRemaining(String sessionId) {
        var state = voiceGateway.getSessionState(sessionId);
        return state.getAssistRemaining();
    }

    @Override
    public int getAssistLimit(String sessionId) {
        var state = voiceGateway.getSessionState(sessionId);
        // 如果状态中没有设置，返回默认值
        return state.getTotalQuestions() > 0 ? DEFAULT_ASSIST_LIMIT : DEFAULT_ASSIST_LIMIT;
    }

    @Override
    public boolean canAssist(String sessionId) {
        int remaining = getAssistRemaining(sessionId);
        return remaining > 0;
    }
}
