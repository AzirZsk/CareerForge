package com.landit.common.service.impl;

import com.landit.common.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * AI 服务实现类
 * 基于 Spring AI Alibaba 实现大模型对话能力
 *
 * @author Azir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final ChatClient chatClient;

    @Override
    public String chat(String prompt) {
        log.debug("AI对话请求: {}", prompt);
        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        log.debug("AI对话响应: {}", response);
        return response;
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        log.debug("AI对话请求 - 系统提示: {}, 用户输入: {}", systemPrompt, userPrompt);
        String response = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
        log.debug("AI对话响应: {}", response);
        return response;
    }

    @Override
    public Flux<String> chatStream(String prompt) {
        log.debug("AI流式对话请求: {}", prompt);
        return chatClient.prompt()
                .user(prompt)
                .stream()
                .content();
    }

    @Override
    public Flux<String> chatStream(String systemPrompt, String userPrompt) {
        log.debug("AI流式对话请求 - 系统提示: {}, 用户输入: {}", systemPrompt, userPrompt);
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .stream()
                .content();
    }

    @Override
    public String chatWithJsonOutput(String prompt, String jsonSchema) {
        log.debug("AI JSON输出请求 - 提示: {}, Schema: {}", prompt, jsonSchema);
        String enhancedPrompt = buildJsonPrompt(prompt, jsonSchema);
        return chat(enhancedPrompt);
    }

    @Override
    public String chatWithJsonOutput(String systemPrompt, String userPrompt, String jsonSchema) {
        log.debug("AI JSON输出请求 - 系统提示: {}, 用户输入: {}, Schema: {}", systemPrompt, userPrompt, jsonSchema);
        String enhancedSystemPrompt = systemPrompt + "\n\n请确保你的回复是有效的JSON格式。";
        String enhancedUserPrompt = buildJsonPrompt(userPrompt, jsonSchema);
        return chat(enhancedSystemPrompt, enhancedUserPrompt);
    }

    /**
     * 构建 JSON 输出提示
     */
    private String buildJsonPrompt(String prompt, String jsonSchema) {
        StringBuilder sb = new StringBuilder();
        sb.append(prompt);
        sb.append("\n\n请按照以下JSON Schema格式返回结果：\n");
        sb.append("```json\n");
        sb.append(jsonSchema);
        sb.append("\n```\n");
        sb.append("注意：只返回JSON数据，不要包含其他说明文字。");
        return sb.toString();
    }

}
