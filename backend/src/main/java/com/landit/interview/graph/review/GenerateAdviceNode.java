package com.landit.interview.graph.review;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.util.ChatClientHelper;
import com.landit.common.util.JsonParseHelper;
import com.landit.interview.graph.review.dto.ListAdviceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

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
        // 使用结构化 DTO 接收响应（JSON Schema 约束）
        ListAdviceResponse response = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, ListAdviceResponse.class
        );
        log.info("改进建议生成完成");
        // 序列化为 JSON 存储
        String adviceJson = JsonParseHelper.toJsonString(response.getAdviceList());
        return buildNodeResult(NODE_GENERATE_ADVICE, 100, "改进建议生成完成",
                response.getAdviceList(), STATE_ADVICE_LIST, adviceJson);
    }

}
