package com.careerforge.resume.graph.tailor;

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
import com.careerforge.common.config.prompt.TailorResumePromptProperties;
import com.careerforge.common.config.prompt.PromptConfig;
import com.careerforge.resume.util.TailoredResumeToSectionConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.careerforge.resume.graph.tailor.TailorResumeGraphConstants.*;

/**
 * 职位适配工作流 Graph 配置
 *
 * 工作流步骤：
 * 1. 分析 JD（AnalyzeJD）
 * 2. 匹配简历（MatchResume）
 * 3. 生成定制简历（GenerateTailored）
 *
 * @author Azir
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class TailorResumeGraphConfig {

    private final ChatClient chatClient;
    private final TailorResumePromptProperties promptProperties;
    private final TailoredResumeToSectionConverter sectionConverter;

    /**
     * 定义状态键策略
     */
    @Bean
    public KeyStrategyFactory tailorResumeKeyStrategyFactory() {
        return () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();

            // 输入相关状态
            strategies.put(STATE_RESUME_CONTENT, new ReplaceStrategy());
            strategies.put(STATE_TARGET_POSITION, new ReplaceStrategy());
            strategies.put(STATE_JOB_DESCRIPTION, new ReplaceStrategy());

            // 分析结果
            strategies.put(STATE_JOB_REQUIREMENTS, new ReplaceStrategy());

            // 匹配结果
            strategies.put(STATE_MATCH_ANALYSIS, new ReplaceStrategy());
            strategies.put(STATE_MATCH_SCORE, new ReplaceStrategy());

            // 输出结果
            strategies.put(STATE_TAILORED_RESUME, new ReplaceStrategy());
            strategies.put(STATE_TAILORED_SECTIONS, new ReplaceStrategy());

            // 对比编辑相关
            strategies.put(STATE_BEFORE_SECTION, new ReplaceStrategy());
            strategies.put(STATE_AFTER_SECTION, new ReplaceStrategy());
            strategies.put(STATE_IMPROVEMENT_SCORE, new ReplaceStrategy());

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
     * 创建职位适配工作流 Graph
     */
    @Bean
    public CompiledGraph tailorResumeGraph(KeyStrategyFactory tailorResumeKeyStrategyFactory)
            throws GraphStateException {

        // 创建节点
        AnalyzeJDNode analyzeJDNode = new AnalyzeJDNode(chatClient, promptProperties);
        MatchResumeNode matchResumeNode = new MatchResumeNode(chatClient, promptProperties);
        GenerateTailoredResumeNode generateTailoredNode = new GenerateTailoredResumeNode(
                chatClient, promptProperties, sectionConverter);

        // 构建工作流图
        StateGraph workflow = new StateGraph(GRAPH_TAILOR_RESUME, tailorResumeKeyStrategyFactory)
                // 添加节点
                .addNode(NODE_ANALYZE_JD, AsyncNodeAction.node_async(analyzeJDNode))
                .addNode(NODE_MATCH_RESUME, AsyncNodeAction.node_async(matchResumeNode))
                .addNode(NODE_GENERATE_TAILORED, AsyncNodeAction.node_async(generateTailoredNode))

                // 添加边：START -> 分析JD -> 匹配简历 -> 生成定制简历 -> END
                .addEdge(START, NODE_ANALYZE_JD)
                .addEdge(NODE_ANALYZE_JD, NODE_MATCH_RESUME)
                .addEdge(NODE_MATCH_RESUME, NODE_GENERATE_TAILORED)
                .addEdge(NODE_GENERATE_TAILORED, END);

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
