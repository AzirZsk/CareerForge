package com.landit.interview.graph.review;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.landit.interview.graph.review.ReviewAnalysisGraphConstants.*;

/**
 * 复盘AI分析工作流 Graph 配置
 *
 * 工作流步骤：
 * 1. AI分析面试表现（包含数据收集）
 * 2. 生成改进建议
 *
 * @author Azir
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ReviewAnalysisGraphConfig {

    /**
     * 定义状态键策略
     */
    @Bean
    public KeyStrategyFactory reviewAnalysisKeyStrategyFactory() {
        return () -> {
            Map<String, KeyStrategy> strategies = new HashMap<>();
            // 输入参数
            strategies.put(STATE_INTERVIEW_ID, new com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy());
            strategies.put(STATE_SESSION_TRANSCRIPT, new com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy());
            // 中间结果
            strategies.put(STATE_TRANSCRIPT_ANALYSIS, new com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy());
            strategies.put(STATE_ANALYSIS_RESULT, new com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy());
            strategies.put(STATE_ADVICE_LIST, new com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy());
            // 消息日志
            strategies.put(STATE_MESSAGES, new com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy());
            // 节点输出
            strategies.put(STATE_NODE_OUTPUT, new com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy());
            return strategies;
        };
    }

    /**
     * 创建复盘AI分析工作流 Graph
     */
    @Bean
    public CompiledGraph reviewAnalysisGraph(
            KeyStrategyFactory reviewAnalysisKeyStrategyFactory,
            AnalyzeTranscriptNode analyzeTranscriptNode,
            AnalyzeInterviewNode analyzeInterviewNode,
            GenerateAdviceNode generateAdviceNode
    ) throws GraphStateException {

        // 构建工作流图
        StateGraph workflow = new StateGraph(GRAPH_REVIEW_ANALYSIS, reviewAnalysisKeyStrategyFactory)
                // 添加节点
                .addNode(NODE_ANALYZE_TRANSCRIPT, AsyncNodeAction.node_async(analyzeTranscriptNode))
                .addNode(NODE_ANALYZE_INTERVIEW, AsyncNodeAction.node_async(analyzeInterviewNode))
                .addNode(NODE_GENERATE_ADVICE, AsyncNodeAction.node_async(generateAdviceNode))
                // 添加边：START -> 对话分析 -> AI分析 -> 生成建议 -> END
                .addEdge(START, NODE_ANALYZE_TRANSCRIPT)
                .addEdge(NODE_ANALYZE_TRANSCRIPT, NODE_ANALYZE_INTERVIEW)
                .addEdge(NODE_ANALYZE_INTERVIEW, NODE_GENERATE_ADVICE)
                .addEdge(NODE_GENERATE_ADVICE, END);

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
