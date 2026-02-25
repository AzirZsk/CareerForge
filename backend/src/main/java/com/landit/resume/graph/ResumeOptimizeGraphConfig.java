package com.landit.resume.graph;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncCommandAction;
import com.alibaba.cloud.ai.graph.action.Command;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
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

/**
 * 简历优化工作流 Graph 配置
 *
 * 工作流步骤：
 * 1. 解析简历内容
 * 2. 诊断分析（快速/精准模式）
 * 3. 生成优化建议
 * 4. 模块内容优化（可选）
 * 5. 人工审核（可选）
 * 6. 保存版本
 *
 * @author Azir
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ResumeOptimizeGraphConfig {

    private final ChatClient.Builder chatClientBuilder;

    /**
     * 定义状态键策略
     */
    @Bean
    public KeyStrategyFactory resumeOptimizeKeyStrategyFactory() {
        return () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            // 简历相关状态
            strategies.put("resume_id", new ReplaceStrategy());
            strategies.put("resume_content", new ReplaceStrategy());
            strategies.put("target_position", new ReplaceStrategy());

            // 诊断相关状态
            strategies.put("diagnosis_mode", new ReplaceStrategy()); // quick / precise
            strategies.put("search_results", new ReplaceStrategy());
            strategies.put("diagnosis_result", new ReplaceStrategy());

            // 优化相关状态
            strategies.put("suggestions", new ReplaceStrategy());
            strategies.put("selected_suggestions", new AppendStrategy());
            strategies.put("optimized_sections", new ReplaceStrategy());

            // 流程控制
            strategies.put("current_step", new ReplaceStrategy());
            strategies.put("next_node", new ReplaceStrategy());
            strategies.put("needs_review", new ReplaceStrategy());
            strategies.put("approved", new ReplaceStrategy());

            // 消息日志
            strategies.put("messages", new AppendStrategy());

            return strategies;
        };
    }

    /**
     * 创建简历优化工作流 Graph
     */
    @Bean
    public CompiledGraph resumeOptimizeGraph(KeyStrategyFactory resumeOptimizeKeyStrategyFactory)
            throws GraphStateException {

        // 创建节点（AsyncNodeActionWithConfig 实例可直接用于 addNode）
        ParseResumeNode parseResumeNode = new ParseResumeNode();
        DiagnoseResumeNode diagnoseNode = new DiagnoseResumeNode(chatClientBuilder);
        DiagnosePreciseResumeNode diagnosePreciseNode = new DiagnosePreciseResumeNode(chatClientBuilder);
        GenerateSuggestionsNode generateSuggestionsNode = new GenerateSuggestionsNode(chatClientBuilder);
        OptimizeSectionNode optimizeSectionNode = new OptimizeSectionNode(chatClientBuilder);
        HumanReviewNode humanReviewNode = new HumanReviewNode();
        SaveVersionNode saveVersionNode = new SaveVersionNode();

        // 构建工作流图
        StateGraph workflow = new StateGraph("resume_optimize", resumeOptimizeKeyStrategyFactory)
                // 添加节点
                .addNode("parse_resume", parseResumeNode)
                .addNode("diagnose_quick", diagnoseNode)
                .addNode("diagnose_precise", diagnosePreciseNode)
                .addNode("generate_suggestions", generateSuggestionsNode)
                .addNode("optimize_section", optimizeSectionNode)
                .addNode("human_review", humanReviewNode)
                .addNode("save_version", saveVersionNode)

                // 添加边
                .addEdge(START, "parse_resume")
                .addEdge("parse_resume", "diagnose_quick") // 默认走快速诊断

                // 条件边：根据诊断模式选择分支
                .addConditionalEdges("diagnose_quick",
                        (AsyncCommandAction) (state, config) -> {
                            String mode = state.value("diagnosis_mode").map(m -> (String) m).orElse("quick");
                            if ("precise".equals(mode)) {
                                return CompletableFuture.completedFuture(new Command("diagnose_precise", null));
                            }
                            return CompletableFuture.completedFuture(new Command("generate_suggestions", null));
                        },
                        Map.of(
                                "diagnose_precise", "diagnose_precise",
                                "generate_suggestions", "generate_suggestions"
                        ))

                .addEdge("diagnose_precise", "generate_suggestions")
                .addEdge("generate_suggestions", "optimize_section")

                // 条件边：根据是否需要人工审核
                .addConditionalEdges("optimize_section",
                        (AsyncCommandAction) (state, config) -> {
                            Boolean needsReview = state.value("needs_review").map(n -> (Boolean) n).orElse(false);
                            if (needsReview) {
                                return CompletableFuture.completedFuture(new Command("human_review", null));
                            }
                            return CompletableFuture.completedFuture(new Command("save_version", null));
                        },
                        Map.of(
                                "human_review", "human_review",
                                "save_version", "save_version"
                        ))

                .addEdge("human_review", "save_version")
                .addEdge("save_version", END);

        // 配置持久化和中断点
        MemorySaver memory = new MemorySaver();
        CompileConfig compileConfig = CompileConfig.builder()
                .saverConfig(SaverConfig.builder()
                        .register(memory)
                        .build())
                .interruptBefore("human_review") // 人工审核前中断
                .build();

        return workflow.compile(compileConfig);
    }

}
