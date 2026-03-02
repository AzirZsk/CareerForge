package com.landit.resume.graph;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncCommandAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.Command;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.schema.GraphSchemaRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.landit.resume.graph.ResumeOptimizeGraphConstants.*;

/**
 * 简历优化工作流 Graph 配置
 *
 * 工作流步骤：
 * 1. 诊断分析（快速/精准模式）
 * 2. 生成优化建议
 * 3. 模块内容优化
 * 4. 人工审核
 * 5. 保存版本
 *
 * 注：简历解析（modules/completeness）已在 Controller 层完成，无需单独节点
 *
 * @author Azir
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ResumeOptimizeGraphConfig {

    private final ChatClient.Builder chatClientBuilder;
    private final AIPromptProperties aiPromptProperties;
    private final GraphSchemaRegistry graphSchemaRegistry;

    /**
     * 定义状态键策略
     */
    @Bean
    public KeyStrategyFactory resumeOptimizeKeyStrategyFactory() {
        return () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            // 简历相关状态
            strategies.put(STATE_RESUME_ID, new ReplaceStrategy());
            strategies.put(STATE_RESUME_CONTENT, new ReplaceStrategy());
            strategies.put(STATE_TARGET_POSITION, new ReplaceStrategy());

            // 诊断相关状态
            strategies.put(STATE_DIAGNOSIS_MODE, new ReplaceStrategy());
            strategies.put(STATE_SEARCH_RESULTS, new ReplaceStrategy());
            strategies.put(STATE_DIAGNOSIS_RESULT, new ReplaceStrategy());

            // 优化相关状态
            strategies.put(STATE_SUGGESTIONS, new ReplaceStrategy());
            strategies.put(STATE_SELECTED_SUGGESTIONS, new AppendStrategy());
            strategies.put(STATE_OPTIMIZED_SECTIONS, new ReplaceStrategy());

            // 流程控制
            strategies.put(STATE_CURRENT_STEP, new ReplaceStrategy());
            strategies.put(STATE_NEXT_NODE, new ReplaceStrategy());
            strategies.put(STATE_APPROVED, new ReplaceStrategy());

            // 消息日志
            strategies.put(STATE_MESSAGES, new AppendStrategy());

            return strategies;
        };
    }

    /**
     * 创建简历优化工作流 Graph
     *
     * 工作流步骤：
     * 1. 诊断分析（快速/精准模式）
     * 2. 生成优化建议
     * 3. 模块内容优化
     * 4. 人工审核
     * 5. 保存版本
     *
     * 注：简历解析（modules/completeness）已在 Controller 层完成，无需单独节点
     */
    @Bean
    public CompiledGraph resumeOptimizeGraph(KeyStrategyFactory resumeOptimizeKeyStrategyFactory)
            throws GraphStateException {

        // 创建节点（注入 GraphSchemaRegistry）
        DiagnoseResumeNode diagnoseNode = new DiagnoseResumeNode(chatClientBuilder, aiPromptProperties, graphSchemaRegistry);
        DiagnosePreciseResumeNode diagnosePreciseNode = new DiagnosePreciseResumeNode(chatClientBuilder, aiPromptProperties, graphSchemaRegistry);
        GenerateSuggestionsNode generateSuggestionsNode = new GenerateSuggestionsNode(chatClientBuilder, aiPromptProperties, graphSchemaRegistry);
        OptimizeSectionNode optimizeSectionNode = new OptimizeSectionNode(chatClientBuilder, aiPromptProperties, graphSchemaRegistry);
        HumanReviewNode humanReviewNode = new HumanReviewNode();
        SaveVersionNode saveVersionNode = new SaveVersionNode();

        // 构建工作流图
        StateGraph workflow = new StateGraph(GRAPH_RESUME_OPTIMIZE, resumeOptimizeKeyStrategyFactory)
                // 添加节点（使用 AsyncNodeAction.node_async 包装）
                .addNode(NODE_DIAGNOSE_QUICK, AsyncNodeAction.node_async(diagnoseNode))
                .addNode(NODE_DIAGNOSE_PRECISE, AsyncNodeAction.node_async(diagnosePreciseNode))
                .addNode(NODE_GENERATE_SUGGESTIONS, AsyncNodeAction.node_async(generateSuggestionsNode))
                .addNode(NODE_OPTIMIZE_SECTION, AsyncNodeAction.node_async(optimizeSectionNode))
                .addNode(NODE_HUMAN_REVIEW, AsyncNodeAction.node_async(humanReviewNode))
                .addNode(NODE_SAVE_VERSION, AsyncNodeAction.node_async(saveVersionNode))

                // 添加边：START 直接连接诊断节点
                .addEdge(START, NODE_DIAGNOSE_QUICK)

                // 条件边：根据诊断模式选择分支
                .addConditionalEdges(NODE_DIAGNOSE_QUICK,
                        (AsyncCommandAction) (state, config) -> {
                            String mode = state.value(STATE_DIAGNOSIS_MODE).map(m -> (String) m).orElse(MODE_QUICK);
                            if (MODE_PRECISE.equals(mode)) {
                                return CompletableFuture.completedFuture(new Command(NODE_DIAGNOSE_PRECISE, Map.of()));
                            }
                            return CompletableFuture.completedFuture(new Command(NODE_GENERATE_SUGGESTIONS, Map.of()));
                        },
                        Map.of(
                                NODE_DIAGNOSE_PRECISE, NODE_DIAGNOSE_PRECISE,
                                NODE_GENERATE_SUGGESTIONS, NODE_GENERATE_SUGGESTIONS
                        ))

                .addEdge(NODE_DIAGNOSE_PRECISE, NODE_GENERATE_SUGGESTIONS)
                .addEdge(NODE_GENERATE_SUGGESTIONS, NODE_OPTIMIZE_SECTION)
                .addEdge(NODE_OPTIMIZE_SECTION, NODE_HUMAN_REVIEW)
                .addEdge(NODE_HUMAN_REVIEW, NODE_SAVE_VERSION)
                .addEdge(NODE_SAVE_VERSION, END);

        // 配置持久化和中断点
        MemorySaver memory = new MemorySaver();
        CompileConfig compileConfig = CompileConfig.builder()
                .saverConfig(SaverConfig.builder()
                        .register(memory)
                        .build())
                .interruptBefore(NODE_HUMAN_REVIEW)
                .build();

        return workflow.compile(compileConfig);
    }

}
