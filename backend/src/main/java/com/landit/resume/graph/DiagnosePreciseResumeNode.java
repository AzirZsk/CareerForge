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
 * 简历诊断节点（精准模式）
 * 基于搜索结果进行精准诊断
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class DiagnosePreciseResumeNode implements AsyncNodeActionWithConfig {

    private final ChatClient.Builder chatClientBuilder;
    private final AIPromptProperties aiPromptProperties;

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        log.info("=== 开始简历诊断（精准模式）===");

        String resumeContent = state.value("resume_content").map(v -> (String) v).orElse("{}");
        String targetPosition = state.value("target_position").map(v -> (String) v).orElse("未知岗位");
        String searchResults = state.value("search_results").map(v -> (String) v).orElse("暂无搜索结果");

        ChatClient chatClient = chatClientBuilder.build();
        String prompt = aiPromptProperties.getGraph().getDiagnosePrecise()
                .replace("{targetPosition}", targetPosition)
                .replace("{searchResults}", searchResults)
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

        String diagnosisResult = streamResponse
                .collectList()
                .map(chunks -> String.join("", chunks))
                .block();

        log.info("精准诊断完成");

        // 解析诊断结果
        Map<String, Object> parsedResult = parseDiagnosisResult(diagnosisResult);

        List<String> messages = new ArrayList<>();
        messages.add("完成简历诊断（精准模式，基于市场数据）");

        // 构建节点输出数据（用于 SSE）
        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put("node", "diagnose_precise");
        nodeOutput.put("progress", 50);
        nodeOutput.put("message", "精准诊断完成（基于市场数据分析）");
        nodeOutput.put("data", parsedResult);

        return CompletableFuture.completedFuture(Map.of(
                "diagnosis_result", diagnosisResult,
                "messages", messages,
                "current_step", "diagnose_precise",
                "node_output", nodeOutput,
                "overall_score", parsedResult.get("overallScore"),
                "match_score", parsedResult.get("matchScore"),
                "dimensions", parsedResult.get("dimensions"),
                "market_requirements", parsedResult.get("marketRequirements"),
                "skill_match", parsedResult.get("skillMatch"),
                "issues", parsedResult.get("issues")
        ));
    }

    private Map<String, Object> parseDiagnosisResult(String result) {
        Map<String, Object> parsed = JsonParseHelper.parseToMap(result);
        if (parsed.isEmpty()) {
            return JsonParseHelper.getDefaultDiagnosisResult();
        }
        return parsed;
    }
}
