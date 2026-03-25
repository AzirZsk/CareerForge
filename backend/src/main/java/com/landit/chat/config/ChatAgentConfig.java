package com.landit.chat.config;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.filesystem.FileSystemSkillRegistry;
import com.landit.common.config.AIPromptProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * ChatAgent 配置类
 * 创建用于 AI 聊天的 ReactAgent，支持技能系统
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
     * 创建技能注册表
     * 从项目根目录的 skills/ 文件夹加载技能
     */
    @Bean
    public SkillRegistry skillRegistry() {
        SkillRegistry registry = FileSystemSkillRegistry.builder()
                .projectSkillsDirectory("./skills")
                .build();
        log.info("[SkillRegistry] 初始化完成, 技能数量={}", registry.size());
        return registry;
    }

    /**
     * 创建 Skills Hook
     * 注册 read_skill 工具并注入技能列表到系统提示
     */
    @Bean
    public SkillsAgentHook skillsAgentHook(SkillRegistry skillRegistry) {
        SkillsAgentHook hook = SkillsAgentHook.builder()
                .skillRegistry(skillRegistry)
                .autoReload(true)
                .build();
        log.info("[SkillsAgentHook] 初始化完成");
        return hook;
    }

    /**
     * 创建 AI 聊天 Agent
     * 使用 MemorySaver 维护对话历史（内存存储，服务重启后丢失）
     * 通过 SkillsAgentHook 支持技能系统
     */
    @Bean
    public ReactAgent chatAgent(SkillsAgentHook skillsAgentHook) {
        String systemPrompt = aiPromptProperties.getChat()
                .getAdvisorConfig()
                .getSystemPrompt();
        log.info("[ChatAgent] 初始化 ChatAgent, systemPrompt length={}", systemPrompt.length());
        return ReactAgent.builder()
                .name("chat_advisor")
                .model(chatModel)
                .systemPrompt(systemPrompt)
                .saver(new MemorySaver())
                .hooks(List.of(skillsAgentHook))
                .build();
    }
}
