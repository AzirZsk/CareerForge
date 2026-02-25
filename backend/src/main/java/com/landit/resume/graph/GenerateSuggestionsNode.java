package com.landit.resume.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.util.JsonParseHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 生成优化建议节点
 * 基于诊断结果生成具体的优化建议
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class GenerateSuggestionsNode implements AsyncNodeActionWithConfig {

    private final ChatClient.Builder chatClientBuilder;
    private final AIPromptProperties aiPromptProperties;

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        log.info("=== 生成优化建议 ===");

        String diagnosisResult = state.value("diagnosis_result").map(v -> (String) v).orElse("{}");
        String resumeContent = state.value("resume_content").map(v -> (String) v).orElse("{}");

        ChatClient chatClient = chatClientBuilder.build();
        String prompt = aiPromptProperties.getGraph().getGenerateSuggestions()
                .replace("{diagnosisResult}", diagnosisResult)
                .replace("{resumeContent}", resumeContent);

        // 使用流式请求调用大模型，收集完整响应
        Flux<String> streamResponse = chatClient.prompt()
                .user(prompt)
                .options(DashScopeChatOptions.builder()
                        .multiModel(true)
                        .incrementalOutput(true)
                        .build())
                .stream()
                .content();

        String suggestionsResult = streamResponse
                .collectList()
                .map(chunks -> String.join("", chunks))
                .block();

        log.info("优化建议生成完成");

        // 解析建议结果
        Map<String, Object> parsedResult = parseSuggestionsResult(suggestionsResult);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> suggestions = (List<Map<String, Object>>) parsedResult.getOrDefault("suggestions", new ArrayList<>());

        List<String> messages = new ArrayList<>();
        messages.add("生成优化建议完成");
        messages.add("共生成 " + suggestions.size() + " 条建议");

        // 构建节点输出数据（用于 SSE）
        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put("node", "generate_suggestions");
        nodeOutput.put("progress", 65);
        nodeOutput.put("message", "已生成 " + suggestions.size() + " 条优化建议");
        nodeOutput.put("data", Map.of(
                "suggestions", suggestions,
                "quickWins", parsedResult.get("quickWins"),
                "estimatedImprovement", parsedResult.get("estimatedImprovement"),
                "totalSuggestions", suggestions.size()
        ));

        return CompletableFuture.completedFuture(Map.of(
                "suggestions", suggestionsResult,
                "messages", messages,
                "current_step", "generate_suggestions",
                "node_output", nodeOutput,
                "suggestion_list", suggestions,
                "quick_wins", parsedResult.get("quickWins")
        ));
    }

    private Map<String, Object> parseSuggestionsResult(String result) {
        Map<String, Object> parsed = JsonParseHelper.parseToMap(result);
        if (parsed.isEmpty()) {
            return JsonParseHelper.getDefaultSuggestionsResult();
        }
        return parsed;
    }
}
