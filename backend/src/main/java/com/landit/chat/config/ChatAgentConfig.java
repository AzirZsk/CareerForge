package com.landit.chat.config;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.classpath.ClasspathSkillRegistry;
import com.landit.chat.tools.AddSectionTool;
import com.landit.chat.tools.CreateResumeTool;
import com.landit.chat.tools.DeleteSectionTool;
import com.landit.chat.tools.GetResumeListTool;
import com.landit.chat.tools.GetResumeTool;
import com.landit.chat.tools.GetSectionTool;
import com.landit.chat.tools.SelectResumeTool;
import com.landit.chat.tools.UpdateSectionTool;
import com.landit.common.config.AIPromptProperties;
import com.landit.resume.handler.ResumeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * ChatAgent 配置类
 * 创建用于 AI 聊天的 ReactAgent，支持技能系统和简历操作工具
 *
 * @author Azir
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ChatAgentConfig {

    private final ChatModel chatModel;
    private final AIPromptProperties aiPromptProperties;
    private final ResumeHandler resumeHandler;

    /**
     * 创建技能注册表
     * 从 classpath:skills/ 加载技能（打包进 JAR）
     */
    @Bean
    public SkillRegistry skillRegistry() {
        SkillRegistry registry = ClasspathSkillRegistry.builder()
                .classpathPath("skills")
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
     * 创建简历操作工具列表
     * 注意：不注册为 Bean，避免被 Spring AI 的 toolCallbackResolver 自动扫描导致循环依赖
     */
    private List<ToolCallback> createResumeTools() {
        List<ToolCallback> tools = List.of(
            GetResumeTool.createCallback(resumeHandler),
            GetSectionTool.createCallback(resumeHandler),
            UpdateSectionTool.createCallback(resumeHandler),
            AddSectionTool.createCallback(resumeHandler),
            DeleteSectionTool.createCallback(resumeHandler),
            CreateResumeTool.createCallback(resumeHandler),
            GetResumeListTool.createCallback(resumeHandler),
            SelectResumeTool.createCallback(resumeHandler)
        );
        log.info("[ResumeTools] 创建 {} 个简历工具", tools.size());
        return tools;
    }

    /**
     * 创建对话记忆存储（内存）
     * 提取为Bean方便Service层检查是否服务重启后丢失了上下文
     */
    @Bean
    public MemorySaver chatMemorySaver() {
        return new MemorySaver();
    }

    /**
     * 创建 AI 聊天 Agent
     * 使用共享的 MemorySaver 维护对话上下文
     * 通过 SkillsAgentHook 支持技能系统
     * 通过 tools 注入简历操作工具
     */
    @Bean
    public ReactAgent chatAgent(SkillsAgentHook skillsAgentHook, MemorySaver chatMemorySaver) {
        String systemPrompt = aiPromptProperties.getChat()
                .getAdvisorConfig()
                .getSystemPrompt();

        // 在方法内部创建工具，避免循环依赖
        List<ToolCallback> resumeTools = createResumeTools();

        log.info("[ChatAgent] 初始化 ChatAgent, systemPrompt length={}, tools count={}",
                systemPrompt.length(), resumeTools.size());
        return ReactAgent.builder()
                .name("chat_advisor")
                .model(chatModel)
                .enableLogging(true)
                .systemPrompt(systemPrompt)
                .saver(chatMemorySaver)
                .hooks(List.of(skillsAgentHook))
                .tools(resumeTools)
                .build();
    }
}
