package com.landit.interview.graph.review;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.landit.common.util.ChatClientHelper;
import com.landit.common.util.JsonParseHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.landit.interview.graph.review.ReviewAnalysisGraphConstants.*;

/**
 * AI分析面试表现节点
 * 根据收集的数据分析面试表现
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzeInterviewNode implements NodeAction {

    private final ChatClient chatClient;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始AI分析面试表现 ===");
        String collectedData = (String) state.value(STATE_COLLECTED_DATA).orElse("{}");
        String systemPrompt = """
                你是一位专业的面试复盘分析师。请根据收集的面试数据，分析面试表现：

                分析维度：
                1. 整体表现评估（优秀/良好/一般/待改进）
                2. 优势亮点（表现好的方面）
                3. 不足之处（需要改进的方面）
                4. 轮次分析（各轮次的表现评价）
                5. 综合建议

                请以JSON格式返回结果，包含以下字段：
                - overallPerformance: 整体表现评级
                - strengths: 优势亮点列表
                - weaknesses: 不足之处列表
                - roundAnalysis: 各轮次分析
                - summary: 综合总结
                """;
        String userPrompt = "请分析以下面试数据：\n" + collectedData;
        String analysisResult = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, String.class
        );
        log.info("AI分析完成");
        List<String> messages = new ArrayList<>();
        messages.add("完成AI分析");
        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put(OUTPUT_NODE, NODE_ANALYZE_INTERVIEW);
        nodeOutput.put(OUTPUT_PROGRESS, 70);
        nodeOutput.put(OUTPUT_MESSAGE, "AI分析完成");
        nodeOutput.put(OUTPUT_DATA, analysisResult);
        Map<String, Object> result = new HashMap<>();
        result.put(STATE_ANALYSIS_RESULT, analysisResult);
        result.put(STATE_MESSAGES, messages);
        result.put(STATE_NODE_OUTPUT, nodeOutput);
        return result;
    }

}
