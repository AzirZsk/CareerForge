package com.landit.interview.voice.handler;

import com.landit.interview.voice.dto.AssistSSEEvent;
import reactor.core.publisher.Flux;

/**
 * AI 助手 Agent 处理器接口
 * 处理面试过程中的快捷求助功能
 *
 * @author Azir
 */
public interface AssistantAgentHandler {

    /**
     * 处理快捷求助
     *
     * @param sessionId      会话 ID
     * @param assistType     求助类型：GIVE_HINTS, EXPLAIN_CONCEPT, POLISH_ANSWER  FREE_QUESTION
     * @param userQuestion   用户问题（自由提问时使用）
     * @param candidateDraft 候选人草稿（润色时使用）
     * @return SSE 事件流
     */
    Flux<AssistSSEEvent> handleAssist(
            String sessionId,
            String assistType,
            String userQuestion,
            String candidateDraft
    );
}
