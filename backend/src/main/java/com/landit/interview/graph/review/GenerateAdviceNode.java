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
 * 生成改进建议节点
 * 根据分析结果生成具体的改进建议
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GenerateAdviceNode implements NodeAction {

    private final ChatClient chatClient;
    private final AIPromptProperties aiPromptProperties;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始生成改进建议 ===");
        String analysisResult = (String) state.value(STATE_ANALYSIS_RESULT).orElse("{}");
        AIPromptProperties.PromptConfig config = aiPromptProperties.getReviewGraph().getGenerateAdviceConfig();
        String systemPrompt = config.getSystemPrompt();
        String userPrompt = config.getUserPromptTemplate().replace("{analysisResult}", analysisResult);
        String adviceJson = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, String.class
        );
        log.info("改进建议生成完成");
        List<String> messages = new ArrayList<>();
        messages.add("完成改进建议生成");
        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put(OUTPUT_NODE, NODE_GENERATE_ADVICE);
        nodeOutput.put(OUTPUT_PROGRESS, 100);
        nodeOutput.put(OUTPUT_MESSAGE, "改进建议生成完成");
        nodeOutput.put(OUTPUT_DATA, adviceJson);
        Map<String, Object> result = new HashMap<>();
        result.put(STATE_ADVICE_LIST, adviceJson);
        result.put(STATE_MESSAGES, messages);
        result.put(STATE_NODE_OUTPUT, nodeOutput);
        return result;
    }

}
