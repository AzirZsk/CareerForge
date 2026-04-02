package com.landit.interview.graph.review;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.util.ChatClientHelper;
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
    private final AIPromptProperties aiPromptProperties;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始AI分析面试表现 ===");
        String collectedData = (String) state.value(STATE_COLLECTED_DATA).orElse("{}");
        AIPromptProperties.PromptConfig config = aiPromptProperties.getReviewGraph().getAnalyzeInterviewConfig();
        String systemPrompt = config.getSystemPrompt();
        String userPrompt = config.getUserPromptTemplate().replace("{collectedData}", collectedData);
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
