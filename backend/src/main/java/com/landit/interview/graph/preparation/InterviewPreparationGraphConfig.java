package com.landit.interview.graph.preparation;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.landit.common.config.AIPromptProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.landit.interview.graph.preparation.InterviewPreparationGraphConstants.*;

/**
 * 面试准备助手工作流 Graph 配置
 *
 * 工作流步骤（简化版，所有节点顺序执行）：
 * 1. 检查公司（如需要则调研）
 * 2. 公司调研
 * 3. 检查职位（如需要则分析）
 * 4. JD分析
 * 5. 生成准备事项
 *
 * @author Azir
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class InterviewPreparationGraphConfig {

    private final ChatClient chatClient;
    private final AIPromptProperties aiPromptProperties;

    /**
     * 定义状态键策略
     */
    @Bean
    public KeyStrategyFactory interviewPreparationKeyStrategyFactory() {
        return () -> {
            Map<String, KeyStrategy> strategies = new HashMap<>();
            // 输入参数
            strategies.put(STATE_INTERVIEW_ID, new com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy());
            strategies.put(STATE_COMPANY_NAME, new com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy());
            strategies.put(STATE_POSITION_TITLE, new com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy());
            strategies.put(STATE_JD_CONTENT, new com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy());
            // 中间结果
            strategies.put(STATE_COMPANY_ID, new com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy());
            strategies.put(STATE_NEED_COMPANY_RESEARCH, new com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy());
            strategies.put(STATE_JOB_POSITION_ID, new com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy());
            strategies.put(STATE_NEED_JD_ANALYSIS, new com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy());
            strategies.put(STATE_COMPANY_RESEARCH_RESULT, new com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy());
            strategies.put(STATE_JD_ANALYSIS_RESULT, new com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy());
            strategies.put(STATE_PREPARATION_ITEMS, new com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy());
            // 流程控制
            strategies.put(STATE_CURRENT_STEP, new com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy());
            // 消息日志
            strategies.put(STATE_MESSAGES, new com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy());
            // 节点输出
            strategies.put(STATE_NODE_OUTPUT, new com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy());
            return strategies;
        };
    }

    /**
     * 创建面试准备助手工作流 Graph
     * 简化版：所有节点顺序执行，节点内部判断是否需要执行操作
     */
    @Bean
    public CompiledGraph interviewPreparationGraph(
            KeyStrategyFactory interviewPreparationKeyStrategyFactory,
            CheckCompanyNode checkCompanyNode,
            CompanyResearchNode companyResearchNode,
            CheckJobPositionNode checkJobPositionNode,
            JDAnalysisNode jdAnalysisNode,
            GeneratePreparationNode generatePreparationNode
    ) throws GraphStateException {

        // 构建工作流图（简化版：顺序执行）
        StateGraph workflow = new StateGraph(GRAPH_INTERVIEW_PREPARATION, interviewPreparationKeyStrategyFactory)
                // 添加节点
                .addNode(NODE_CHECK_COMPANY, AsyncNodeAction.node_async(checkCompanyNode))
                .addNode(NODE_COMPANY_RESEARCH, AsyncNodeAction.node_async(companyResearchNode))
                .addNode(NODE_CHECK_JOB_POSITION, AsyncNodeAction.node_async(checkJobPositionNode))
                .addNode(NODE_JD_ANALYSIS, AsyncNodeAction.node_async(jdAnalysisNode))
                .addNode(NODE_GENERATE_PREPARATION, AsyncNodeAction.node_async(generatePreparationNode))
                // 添加边：START -> 检查公司 -> 公司调研 -> 检查职位 -> JD分析 -> 生成准备 -> END
                .addEdge(START, NODE_CHECK_COMPANY)
                .addEdge(NODE_CHECK_COMPANY, NODE_COMPANY_RESEARCH)
                .addEdge(NODE_COMPANY_RESEARCH, NODE_CHECK_JOB_POSITION)
                .addEdge(NODE_CHECK_JOB_POSITION, NODE_JD_ANALYSIS)
                .addEdge(NODE_JD_ANALYSIS, NODE_GENERATE_PREPARATION)
                .addEdge(NODE_GENERATE_PREPARATION, END);

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
