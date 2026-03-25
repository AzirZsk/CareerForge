package com.landit.chat.config;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.landit.common.config.AIPromptProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChatAgent 配置类
 * 创建用于 AI 聊天的 ReactAgent
 *
 * @author Azir
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ChatAgentConfig {

    private final ChatModel chatModel;
    private final AIPromptProperties aiPromptProperties;

    /**
     * 创建 AI 聊天 Agent
     * 使用 MemorySaver 维护对话历史（内存存储，服务重启后丢失）
     */
    @Bean
    public ReactAgent chatAgent() {
        String systemPrompt = aiPromptProperties.getChat()
                .getAdvisorConfig()
                .getSystemPrompt();
        log.info("[ChatAgent] 初始化 ChatAgent, systemPrompt length={}", systemPrompt.length());
        return ReactAgent.builder()
                .name("chat_advisor")
                .model(chatModel)
                .systemPrompt(systemPrompt)
                .saver(new MemorySaver())
                .build();
    }
}
