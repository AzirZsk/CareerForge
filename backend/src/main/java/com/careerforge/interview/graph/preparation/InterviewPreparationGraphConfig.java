package com.careerforge.interview.graph.preparation;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.careerforge.interview.graph.preparation.InterviewPreparationGraphConstants.*;

/**
 * 面试准备助手工作流 Graph 配置
 *
 * 简化版工作流（3个节点，顺序执行）：
 * 1. 公司调研节点 - 检查缓存，如需要则执行AI调研
 * 2. JD分析节点 - 检查缓存，如需要则执行AI分析
 * 3. 生成准备事项节点 - 根据调研和分析结果生成准备事项
 *
 * @author Azir
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class InterviewPreparationGraphConfig {

    /**
     * 定义状态键策略
     */
    @Bean
    public KeyStrategyFactory interviewPreparationKeyStrategyFactory() {
        return () -> {
            Map<String, KeyStrategy> strategies = new HashMap<>();
            // 输入参数
            strategies.put(STATE_INTERVIEW_ID, new ReplaceStrategy());
            strategies.put(STATE_COMPANY_NAME, new ReplaceStrategy());
            strategies.put(STATE_POSITION_TITLE, new ReplaceStrategy());
            strategies.put(STATE_JD_CONTENT, new ReplaceStrategy());
            strategies.put(STATE_RESUME_CONTENT, new ReplaceStrategy());
            // 中间结果
            strategies.put(STATE_COMPANY_ID, new ReplaceStrategy());
            strategies.put(STATE_COMPANY_RESEARCH_RESULT, new ReplaceStrategy());
            strategies.put(STATE_JOB_POSITION_ID, new ReplaceStrategy());
            strategies.put(STATE_JD_ANALYSIS_RESULT, new ReplaceStrategy());
            strategies.put(STATE_PREPARATION_ITEMS, new ReplaceStrategy());
            // 流程控制
            strategies.put(STATE_CURRENT_STEP, new ReplaceStrategy());
            // 消息日志
            strategies.put(STATE_MESSAGES, new AppendStrategy());
            // 节点输出
            strategies.put(STATE_NODE_OUTPUT, new ReplaceStrategy());
            return strategies;
        };
    }

    /**
     * 创建面试准备助手工作流 Graph
     * 简化版：3个节点顺序执行，节点内部判断是否需要执行操作
     */
    @Bean
    public CompiledGraph interviewPreparationGraph(
            KeyStrategyFactory interviewPreparationKeyStrategyFactory,
            CompanyResearchNode companyResearchNode,
            JDAnalysisNode jdAnalysisNode,
            GeneratePreparationNode generatePreparationNode
    ) throws GraphStateException {

        // 构建工作流图（简化版：3个节点顺序执行）
        StateGraph workflow = new StateGraph(GRAPH_INTERVIEW_PREPARATION, interviewPreparationKeyStrategyFactory)
                // 添加节点（3个）
                .addNode(NODE_COMPANY_RESEARCH, AsyncNodeAction.node_async(companyResearchNode))
                .addNode(NODE_JD_ANALYSIS, AsyncNodeAction.node_async(jdAnalysisNode))
                .addNode(NODE_GENERATE_PREPARATION, AsyncNodeAction.node_async(generatePreparationNode))
                // 添加边（顺序执行，无条件边）
                .addEdge(START, NODE_COMPANY_RESEARCH)
                .addEdge(NODE_COMPANY_RESEARCH, NODE_JD_ANALYSIS)
                .addEdge(NODE_JD_ANALYSIS, NODE_GENERATE_PREPARATION)
                .addEdge(NODE_GENERATE_PREPARATION, END);

        // 配置持久化
        MemorySaver memory = new MemorySaver();
        CompileConfig compileConfig = CompileConfig.builder()
                .saverConfig(SaverConfig.builder()
                        .register(memory)
                        .build())
                .build();

        log.info("面试准备工作流初始化完成（3节点简化版）");
        return workflow.compile(compileConfig);
    }

}
