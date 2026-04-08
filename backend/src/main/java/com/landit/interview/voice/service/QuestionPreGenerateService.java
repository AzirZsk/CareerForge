package com.landit.interview.voice.service;

import com.landit.interview.voice.dto.PreGenerateContext;
import com.landit.interview.voice.dto.PreGeneratedQuestion;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 面试问题预生成服务
 * 在面试开始前预生成问题，实现零延迟推送
 *
 * @author Azir
 */
public interface QuestionPreGenerateService {

    /**
     * 异步预生成开场白 + 第一个问题
     * 在创建会话后立即调用，预生成完成后通过回调通知
     *
     * @param sessionId 会话ID
     * @param context   预生成上下文（JD、简历、面试官风格）
     * @return 预生成任务的 Future，可用于等待或回调
     */
    CompletableFuture<PreGeneratedQuestion> preGenerateFirstQuestion(String sessionId, PreGenerateContext context);

    /**
     * 异步预生成下一个问题
     * 在候选人回答当前问题时并行调用
     *
     * @param sessionId         会话ID
     * @param nextQuestionIndex 下一个问题序号
     */
    void preGenerateNextQuestion(String sessionId, int nextQuestionIndex);

    /**
     * 获取缓存的预生成问题
     *
     * @param sessionId     会话ID
     * @param questionIndex 问题序号
     * @return 预生成问题（如果存在且未使用）
     */
    Optional<PreGeneratedQuestion> getCachedQuestion(String sessionId, int questionIndex);

    /**
     * 标记问题已使用
     *
     * @param sessionId     会话ID
     * @param questionIndex 问题序号
     */
    void markQuestionUsed(String sessionId, int questionIndex);

    /**
     * 清理会话的所有缓存
     *
     * @param sessionId 会话ID
     */
    void clearCache(String sessionId);

    /**
     * 检查预生成是否完成
     *
     * @param sessionId 会话ID
     * @return true 表示第一个问题已预生成完成
     */
    boolean isPreGenerationComplete(String sessionId);
}
