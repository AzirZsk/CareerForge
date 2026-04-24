package com.careerforge.resume.graph.rewrite;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.careerforge.common.config.prompt.RewritePromptProperties;
import com.careerforge.common.config.prompt.PromptConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.careerforge.resume.graph.rewrite.RewriteGraphConstants.*;

/**
 * 简历风格改写工作流 Graph 配置
 *
 * 工作流步骤：
 * 1. 分析参考简历风格
 * 2. 生成风格差异建议
 * 3. 应用风格改写
 *
 * @author Azir
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RewriteGraphConfig {

    private final ChatClient chatClient;
    private final RewritePromptProperties promptProperties;

    /**
     * 定义状态键策略
     */
    @Bean
    public KeyStrategyFactory resumeRewriteKeyStrategyFactory() {
        return () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            // 简历相关状态
            strategies.put(STATE_RESUME_CONTENT, new ReplaceStrategy());
            strategies.put(STATE_REFERENCE_CONTENT, new ReplaceStrategy());
            // 风格分析结果
            strategies.put(STATE_STYLE_ANALYSIS, new ReplaceStrategy());
            // 改写策略与结果
            strategies.put(STATE_OPTIMIZED_SECTIONS, new ReplaceStrategy());
            // 变更与评分
            strategies.put(STATE_CHANGES, new ReplaceStrategy());
            strategies.put(STATE_IMPROVEMENT_SCORE, new ReplaceStrategy());
            // 流程控制
            strategies.put(STATE_CURRENT_STEP, new ReplaceStrategy());
            // 消息日志
            strategies.put(STATE_MESSAGES, new AppendStrategy());
            // ID映射表
            strategies.put(STATE_SECTION_ID_MAP, new ReplaceStrategy());
            // 节点输出
            strategies.put(STATE_NODE_OUTPUT, new ReplaceStrategy());
            return strategies;
        };
    }

    /**
     * 创建简历风格改写工作流 Graph
     */
    @Bean
    public CompiledGraph resumeRewriteGraph(KeyStrategyFactory resumeRewriteKeyStrategyFactory)
            throws GraphStateException {
        // 创建节点
        AnalyzeStyleNode analyzeStyleNode = new AnalyzeStyleNode(chatClient, promptProperties);
        GenerateStyleDiffNode generateStyleDiffNode = new GenerateStyleDiffNode(chatClient, promptProperties);
        RewriteSectionNode rewriteSectionNode = new RewriteSectionNode(chatClient, promptProperties);
        // 构建工作流图
        StateGraph workflow = new StateGraph(GRAPH_RESUME_REWRITE, resumeRewriteKeyStrategyFactory)
                .addNode(NODE_ANALYZE_STYLE, AsyncNodeAction.node_async(analyzeStyleNode))
                .addNode(NODE_GENERATE_STYLE_DIFF, AsyncNodeAction.node_async(generateStyleDiffNode))
                .addNode(NODE_REWRITE_SECTION, AsyncNodeAction.node_async(rewriteSectionNode))
                .addEdge(START, NODE_ANALYZE_STYLE)
                .addEdge(NODE_ANALYZE_STYLE, NODE_GENERATE_STYLE_DIFF)
                .addEdge(NODE_GENERATE_STYLE_DIFF, NODE_REWRITE_SECTION)
                .addEdge(NODE_REWRITE_SECTION, END);
        // 配置持久化
        MemorySaver memory = new MemorySaver();
        CompileConfig compileConfig = CompileConfig.builder()
                .saverConfig(SaverConfig.builder()
                        .register(memory)
                        .build())
                .build();

        return workflow.compile(compileConfig);
    }
}
