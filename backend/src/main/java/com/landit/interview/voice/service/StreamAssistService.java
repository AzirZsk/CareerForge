package com.landit.interview.voice.service;

import com.landit.interview.voice.dto.AssistSSEEvent;
import reactor.core.publisher.Flux;

/**
 * 流式求助服务接口
 * 处理面试过程中的快捷求助功能
 *
 * @author Azir
 */
public interface StreamAssistService {

    /**
     * 流式求助
     *
     * @param sessionId      会话 ID
     * @param assistType      求助类型
     * @param question        自由提问内容
     * @param candidateDraft  候选人草稿
     * @return SSE 事件流
     */
    Flux<AssistSSEEvent> streamAssist(
            String sessionId,
            String assistType,
            String question,
            String candidateDraft
    );

    /**
     * 获取剩余求助次数
     *
     * @param sessionId 会话 ID
     * @return 剩余次数
     */
    int getAssistRemaining(String sessionId);

    /**
     * 获取求助次数上限
     *
     * @param sessionId 会话 ID
     * @return 上限
     */
    int getAssistLimit(String sessionId);

    /**
     * 检查是否可以求助
     *
     * @param sessionId 会话 ID
     * @return true 表示可以求助
     */
    boolean canAssist(String sessionId);
}
