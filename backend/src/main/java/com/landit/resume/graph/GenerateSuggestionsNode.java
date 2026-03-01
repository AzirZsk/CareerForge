package com.landit.resume.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.RunnableConfig;
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
import java.util.concurrent.CompletableFuture;

/**
 * 生成优化建议节点
 * 基于诊断结果生成具体的优化建议，使用 JSON Schema 约束输出格式
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class GenerateSuggestionsNode implements AsyncNodeActionWithConfig {

    private final ChatClient.Builder chatClientBuilder;
    private final AIPromptProperties aiPromptProperties;
    private final GraphSchemaRegistry graphSchemaRegistry;

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        log.info("=== 生成优化建议 ===");

        String diagnosisResult = state.value("diagnosis_result").map(v -> (String) v).orElse("{}");
        String resumeContent = state.value("resume_content").map(v -> (String) v).orElse("{}");

        String prompt = aiPromptProperties.getGraph().getGenerateSuggestions()
                .replace("{diagnosisResult}", diagnosisResult)
                .replace("{resumeContent}", resumeContent);

        // 使用 JSON Schema 约束调用大模型
        String suggestionsResult = ChatClientHelper.callStreamAndCollectWithSchema(
                chatClientBuilder,
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
                "generate_suggestions",
                65,
                "已生成 " + suggestionCount + " 条优化建议",
                Map.of(
                        "suggestions", suggestions,
                        "quickWins", response.getQuickWins(),
                        "estimatedImprovement", response.getEstimatedImprovement(),
                        "totalSuggestions", suggestionCount
                )
        );

        return CompletableFuture.completedFuture(Map.of(
                "suggestions", suggestionsResult,
                "messages", messages,
                "current_step", "generate_suggestions",
                "node_output", nodeOutput,
                "suggestion_list", suggestions,
                "quick_wins", response.getQuickWins()
        ));
    }
}
