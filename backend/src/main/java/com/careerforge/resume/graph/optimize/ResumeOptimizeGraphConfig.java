package com.careerforge.resume.graph.optimize;

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
import com.careerforge.common.config.prompt.ResumeOptimizePromptProperties;
import com.careerforge.common.config.prompt.PromptConfig;
import com.careerforge.resume.service.ResumeService;
import com.careerforge.resume.service.ResumeSuggestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.careerforge.resume.graph.optimize.ResumeOptimizeGraphConstants.*;

/**
 * 简历优化工作流 Graph 配置
 *
 * 工作流步骤：
 * 1. 诊断分析
 * 2. 生成优化建议
 * 3. 模块内容优化
 *
 * 注：简历解析（modules/completeness）已在 Controller 层完成，无需单独节点
 *
 * @author Azir
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ResumeOptimizeGraphConfig {

    private final ChatClient chatClient;
    private final ResumeOptimizePromptProperties promptProperties;
    private final ResumeService resumeService;
    private final ResumeSuggestionService resumeSuggestionService;
    private final DiagnoseResumeNode diagnoseResumeNode;

    /**
     * 定义状态键策略
     */
    @Bean
    public KeyStrategyFactory resumeOptimizeKeyStrategyFactory() {
        return () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            // 简历相关状态
            strategies.put(STATE_RESUME_CONTENT, new ReplaceStrategy());

            // 诊断相关状态
            strategies.put(STATE_DIAGNOSIS_MODE, new ReplaceStrategy());
            strategies.put(STATE_DIAGNOSIS_RESULT, new ReplaceStrategy());

            // 优化相关状态
            strategies.put(STATE_SUGGESTIONS, new ReplaceStrategy());
            strategies.put(STATE_SELECTED_SUGGESTIONS, new AppendStrategy());
            strategies.put(STATE_OPTIMIZED_SECTIONS, new ReplaceStrategy());

            // 流程控制
            strategies.put(STATE_CURRENT_STEP, new ReplaceStrategy());
            strategies.put(STATE_NEXT_NODE, new ReplaceStrategy());

            // 消息日志
            strategies.put(STATE_MESSAGES, new AppendStrategy());

            // ID映射表
            strategies.put(STATE_SECTION_ID_MAP, new ReplaceStrategy());

            return strategies;
        };
    }

    /**
     * 创建简历优化工作流 Graph
     *
     * 工作流步骤：
     * 1. 诊断分析
     * 2. 生成优化建议
     * 3. 模块内容优化
     *
     * 注：简历解析（modules/completeness）已在 Controller 层完成，无需单独节点
     */
    @Bean
    public CompiledGraph resumeOptimizeGraph(KeyStrategyFactory resumeOptimizeKeyStrategyFactory)
            throws GraphStateException {

        // 创建节点（注入已配置的 ChatClient Bean）
        GenerateSuggestionsNode generateSuggestionsNode = new GenerateSuggestionsNode(chatClient, promptProperties, resumeSuggestionService);
        OptimizeSectionNode optimizeSectionNode = new OptimizeSectionNode(chatClient, promptProperties);

        // 构建工作流图
        StateGraph workflow = new StateGraph(GRAPH_RESUME_OPTIMIZE, resumeOptimizeKeyStrategyFactory)
                // 添加节点（使用 AsyncNodeAction.node_async 包装）
                .addNode(NODE_DIAGNOSE_QUICK, AsyncNodeAction.node_async(diagnoseResumeNode))
                .addNode(NODE_GENERATE_SUGGESTIONS, AsyncNodeAction.node_async(generateSuggestionsNode))
                .addNode(NODE_OPTIMIZE_SECTION, AsyncNodeAction.node_async(optimizeSectionNode))

                // 添加边：START -> 诊断 -> 生成建议 -> 优化 -> END
                .addEdge(START, NODE_DIAGNOSE_QUICK)
                .addEdge(NODE_DIAGNOSE_QUICK, NODE_GENERATE_SUGGESTIONS)
                .addEdge(NODE_GENERATE_SUGGESTIONS, NODE_OPTIMIZE_SECTION)
                .addEdge(NODE_OPTIMIZE_SECTION, END);

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
