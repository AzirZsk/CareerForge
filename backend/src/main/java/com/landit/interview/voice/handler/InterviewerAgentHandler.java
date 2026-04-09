package com.landit.interview.voice.handler;

import com.landit.interview.voice.dto.VoiceResponse;
import reactor.core.publisher.Flux;

/**
 * 面试官 Agent 处理器接口
 * 处理候选人回答，生成面试官回复
 *
 * @author Azir
 */
public interface InterviewerAgentHandler {

    /**
     * 处理候选人音频
     *
     * @param sessionId  会话 ID
     * @param audioData  音频数据（PCM）
     * @return 响应流（转录 + AI 回复 + 音频）
     */
    Flux<VoiceResponse> handleCandidateAudio(String sessionId, byte[] audioData);

    /**
     * 处理候选人文本输入（非语音模式）
     *
     * @param sessionId 会话 ID
     * @param text      文本内容
     * @return 响应流（AI 回复 + 音频）
     */
    Flux<VoiceResponse> handleCandidateText(String sessionId, String text);

    /**
     * 生成下一个问题
     *
     * @param sessionId 会话 ID
     * @return 响应流（问题文本 + 音频）
     */
    Flux<VoiceResponse> generateNextQuestion(String sessionId);

    /**
     * 生成追问（可选）
     * 当面试官需要对候选人回答进行深入挖掘时调用
     *
     * @param sessionId          会话 ID
     * @param lastQuestion       上一个问题
     * @param candidateAnswer    候选人回答
     * @param conversationHistory 最近对话摘要
     * @return 响应流（追问文本 + 音频）
     */
    Flux<VoiceResponse> generateFollowUpQuestion(
            String sessionId,
            String lastQuestion,
            String candidateAnswer,
            String conversationHistory);
}
