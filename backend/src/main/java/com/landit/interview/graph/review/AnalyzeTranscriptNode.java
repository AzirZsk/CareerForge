package com.landit.interview.graph.review;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.util.ChatClientHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.landit.interview.graph.review.ReviewAnalysisGraphConstants.*;

/**
 * 分析面试对话文本节点
 * 提取问答对，分析问题意图和回答清晰度
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzeTranscriptNode implements NodeAction {

    private final ChatClient chatClient;
    private final AIPromptProperties aiPromptProperties;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始分析面试对话 ===");
        // 获取输入
        String sessionTranscript = (String) state.value(STATE_SESSION_TRANSCRIPT).orElse("");
        // 空文本处理：跳过分析
        if (sessionTranscript == null || sessionTranscript.isBlank()) {
            log.warn("面试对话文本为空，跳过分析");
            return buildResult("{}", "对话文本为空，跳过分析");
        }
        // 调用 AI 分析对话
        AIPromptProperties.PromptConfig config = aiPromptProperties.getReviewGraph().getAnalyzeTranscriptConfig();
        String systemPrompt = config.getSystemPrompt();
        String userPrompt = config.getUserPromptTemplate().replace("{sessionTranscript}", sessionTranscript);
        String transcriptAnalysis = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, String.class
        );
        log.info("面试对话分析完成");
        return buildResult(transcriptAnalysis, "对话分析完成");
    }

    /**
     * 构建节点返回结果
     *
     * @param analysisData 分析结果数据（JSON）
     * @param message      进度消息
     * @return 状态更新Map
     */
    private Map<String, Object> buildResult(String analysisData, String message) {
        // 构建节点输出（用于 SSE 推送）
        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put(OUTPUT_NODE, NODE_ANALYZE_TRANSCRIPT);
        nodeOutput.put(OUTPUT_PROGRESS, 30);
        nodeOutput.put(OUTPUT_MESSAGE, message);
        nodeOutput.put(OUTPUT_DATA, analysisData);
        // 构建状态更新
        Map<String, Object> result = new HashMap<>();
        result.put(STATE_TRANSCRIPT_ANALYSIS, analysisData);
        result.put(STATE_MESSAGES, List.of(message));
        result.put(STATE_NODE_OUTPUT, nodeOutput);
        return result;
    }

}
