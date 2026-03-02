package com.landit.resume.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.schema.GraphSchemaRegistry;
import com.landit.common.util.ChatClientHelper;
import com.landit.common.util.JsonParseHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.landit.resume.graph.ResumeOptimizeGraphConstants.*;

/**
 * 生成优化建议节点
 * 基于诊断结果生成具体的优化建议，使用 JSON Schema 约束输出格式
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class GenerateSuggestionsNode implements NodeAction {

    private final ChatClient chatClient;
    private final AIPromptProperties aiPromptProperties;
    private final GraphSchemaRegistry graphSchemaRegistry;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 生成优化建议 ===");

        String diagnosisResult = state.value(STATE_DIAGNOSIS_RESULT).map(v -> (String) v).orElse(DEFAULT_EMPTY_JSON);
        String resumeContent = state.value(STATE_RESUME_CONTENT).map(v -> (String) v).orElse(DEFAULT_EMPTY_JSON);

        String prompt = aiPromptProperties.getGraph().getGenerateSuggestions()
                .replace("{diagnosisResult}", diagnosisResult)
                .replace("{resumeContent}", resumeContent);

        // 使用 JSON Schema 约束调用大模型
        String suggestionsResult = ChatClientHelper.callStreamAndCollectWithSchema(
                chatClient,
                prompt,
                graphSchemaRegistry.buildSuggestionsSchema(),
                "SuggestionsResponse"
        );

        log.info("优化建议生成完成");

        // 解析建议结果为实体
        GraphSchemaRegistry.SuggestionsResponse response = JsonParseHelper.parseToEntity(
                suggestionsResult,
                GraphSchemaRegistry.SuggestionsResponse.class
        );

        List<?> suggestions = response.getSuggestions() != null ? response.getSuggestions() : List.of();
        int suggestionCount = suggestions.size();

        List<String> messages = new ArrayList<>();
        messages.add("生成优化建议完成");
        messages.add("共生成 " + suggestionCount + " 条建议");

        // 构建节点输出数据（用于 SSE）
        Map<String, Object> nodeOutput = JsonParseHelper.buildNodeOutput(
                NODE_GENERATE_SUGGESTIONS,
                65,
                "已生成 " + suggestionCount + " 条优化建议",
                Map.of(
                        "suggestions", suggestions,
                        "quickWins", response.getQuickWins(),
                        "estimatedImprovement", response.getEstimatedImprovement(),
                        "totalSuggestions", suggestionCount
                )
        );

        return Map.of(
                STATE_SUGGESTIONS, suggestionsResult,
                STATE_MESSAGES, messages,
                STATE_CURRENT_STEP, NODE_GENERATE_SUGGESTIONS,
                STATE_NODE_OUTPUT, nodeOutput,
                STATE_SUGGESTION_LIST, suggestions,
                STATE_QUICK_WINS, response.getQuickWins()
        );
    }
}
