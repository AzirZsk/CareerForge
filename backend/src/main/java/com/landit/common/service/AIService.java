package com.landit.common.service;

import reactor.core.publisher.Flux;

/**
 * AI 服务接口
 * 提供大模型对话能力
 *
 * @author Azir
 */
public interface AIService {

    /**
     * 普通对话
     *
     * @param prompt 用户输入
     * @return AI 回复
     */
    String chat(String prompt);

    /**
     * 带系统提示的对话
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户输入
     * @return AI 回复
     */
    String chat(String systemPrompt, String userPrompt);

    /**
     * 流式对话（SSE）
     *
     * @param prompt 用户输入
     * @return AI 回复流
     */
    Flux<String> chatStream(String prompt);

    /**
     * 带系统提示的流式对话（SSE）
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户输入
     * @return AI 回复流
     */
    Flux<String> chatStream(String systemPrompt, String userPrompt);

    /**
     * 结构化输出（JSON）
     *
     * @param prompt     用户输入
     * @param jsonSchema JSON Schema 描述期望的输出格式
     * @return JSON 格式的 AI 回复
     */
    String chatWithJsonOutput(String prompt, String jsonSchema);

    /**
     * 带系统提示的结构化输出（JSON）
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户输入
     * @param jsonSchema   JSON Schema 描述期望的输出格式
     * @return JSON 格式的 AI 回复
     */
    String chatWithJsonOutput(String systemPrompt, String userPrompt, String jsonSchema);

}
