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
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
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
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
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
            strategies.put(STATE_INTERVIEW_ID, new ReplaceStrategy());
            strategies.put(STATE_COMPANY_NAME, new ReplaceStrategy());
            strategies.put(STATE_POSITION_TITLE, new ReplaceStrategy());
            strategies.put(STATE_JD_CONTENT, new ReplaceStrategy());
            // 中间结果
            strategies.put(STATE_COMPANY_ID, new ReplaceStrategy());
            strategies.put(STATE_NEED_COMPANY_RESEARCH, new ReplaceStrategy());
            strategies.put(STATE_JOB_POSITION_ID, new ReplaceStrategy());
            strategies.put(STATE_NEED_JD_ANALYSIS, new ReplaceStrategy());
            strategies.put(STATE_COMPANY_RESEARCH_RESULT, new ReplaceStrategy());
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

        // 构建工作流图（条件路由版：根据检查结果跳过已处理的节点）
        StateGraph workflow = new StateGraph(GRAPH_INTERVIEW_PREPARATION, interviewPreparationKeyStrategyFactory)
                // 添加节点
                .addNode(NODE_CHECK_COMPANY, AsyncNodeAction.node_async(checkCompanyNode))
                .addNode(NODE_COMPANY_RESEARCH, AsyncNodeAction.node_async(companyResearchNode))
                .addNode(NODE_CHECK_JOB_POSITION, AsyncNodeAction.node_async(checkJobPositionNode))
                .addNode(NODE_JD_ANALYSIS, AsyncNodeAction.node_async(jdAnalysisNode))
                .addNode(NODE_GENERATE_PREPARATION, AsyncNodeAction.node_async(generatePreparationNode))
                // 添加边
                // START -> check_company
                .addEdge(START, NODE_CHECK_COMPANY)
                // check_company 条件边：根据 need_company_research 决定是否执行 company_research
                .addConditionalEdges(NODE_CHECK_COMPANY,
                        edge_async(state -> {
                            boolean needResearch = (Boolean) state.value(STATE_NEED_COMPANY_RESEARCH).orElse(true);
                            log.info("条件路由 - check_company: needResearch={}", needResearch);
                            return needResearch ? ROUTE_NEED_RESEARCH : ROUTE_SKIP_RESEARCH;
                        }),
                        Map.of(
                                ROUTE_NEED_RESEARCH, NODE_COMPANY_RESEARCH,
                                ROUTE_SKIP_RESEARCH, NODE_CHECK_JOB_POSITION
                        ))
                // company_research 固定边：执行完后进入 check_job_position
                .addEdge(NODE_COMPANY_RESEARCH, NODE_CHECK_JOB_POSITION)
                // check_job_position 条件边：根据 need_jd_analysis 决定是否执行 jd_analysis
                .addConditionalEdges(NODE_CHECK_JOB_POSITION,
                        edge_async(state -> {
                            boolean needAnalysis = (Boolean) state.value(STATE_NEED_JD_ANALYSIS).orElse(true);
                            log.info("条件路由 - check_job_position: needAnalysis={}", needAnalysis);
                            return needAnalysis ? ROUTE_NEED_ANALYSIS : ROUTE_SKIP_ANALYSIS;
                        }),
                        Map.of(
                                ROUTE_NEED_ANALYSIS, NODE_JD_ANALYSIS,
                                ROUTE_SKIP_ANALYSIS, NODE_GENERATE_PREPARATION
                        ))
                // jd_analysis 固定边：执行完后进入 generate_preparation
                .addEdge(NODE_JD_ANALYSIS, NODE_GENERATE_PREPARATION)
                // generate_preparation 固定边：结束
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
