package com.landit.resume.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.landit.common.util.JsonParseHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 模块内容优化节点
 * 对选定的简历模块进行 AI 优化
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class OptimizeSectionNode implements AsyncNodeActionWithConfig {

    private final ChatClient.Builder chatClientBuilder;

    private static final String OPTIMIZE_SECTION_PROMPT = """
            你是一位专业的简历内容优化专家。

            ## 任务
            优化以下简历模块的内容。

            ## 模块类型
            %s

            ## 目标岗位
            %s

            ## 原始内容
            %s

            ## 优化建议
            %s

            请以JSON格式返回，格式如下：
            {
              "optimizedContent": {
                "sections": [
                  {
                    "id": "xxx",
                    "type": "PROJECT",
                    "title": "项目名称",
                    "content": "优化后的内容",
                    "score": 90
                  }
                ]
              },
              "changes": [
                {
                  "sectionId": "xxx",
                  "sectionType": "PROJECT",
                  "field": "description",
                  "before": "原始内容",
                  "after": "优化后内容",
                  "reason": "优化原因"
                }
              ],
              "improvementScore": 15,
              "tips": ["补充提示1", "补充提示2"],
              "confidence": "high"
            }
            """;

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        log.info("=== 模块内容优化 ===");

        String resumeContent = state.value("resume_content").map(v -> (String) v).orElse("{}");
        String targetPosition = state.value("target_position").map(v -> (String) v).orElse("未知岗位");
        String suggestions = state.value("suggestions").map(v -> (String) v).orElse("[]");

        // 获取用户选择的要优化的模块（默认优化全部）
        String sectionType = state.value("section_to_optimize").map(v -> (String) v).orElse("all");

        ChatClient chatClient = chatClientBuilder.build();
        String prompt = String.format(OPTIMIZE_SECTION_PROMPT, sectionType, targetPosition, resumeContent, suggestions);

        String optimizeResult = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        log.info("模块优化完成");

        // 解析优化结果
        Map<String, Object> parsedResult = parseOptimizeResult(optimizeResult);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> changes = (List<Map<String, Object>>) parsedResult.getOrDefault("changes", new ArrayList<>());

        List<String> messages = new ArrayList<>();
        messages.add("模块内容优化完成");
        messages.add("共优化 " + changes.size() + " 处");

        // 根据优化结果的置信度决定是否需要人工审核
        String confidence = (String) parsedResult.getOrDefault("confidence", "medium");
        boolean needsReview = !"high".equals(confidence);

        // 构建节点输出数据（用于 SSE）
        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put("node", "optimize_section");
        nodeOutput.put("progress", 80);
        nodeOutput.put("message", "内容优化完成，共 " + changes.size() + " 处变更");
        nodeOutput.put("data", Map.of(
                "changes", changes,
                "improvementScore", parsedResult.get("improvementScore"),
                "tips", parsedResult.get("tips"),
                "confidence", confidence,
                "needsReview", needsReview,
                "changeCount", changes.size()
        ));

        return CompletableFuture.completedFuture(Map.of(
                "optimized_sections", optimizeResult,
                "needs_review", needsReview,
                "messages", messages,
                "current_step", "optimize_section",
                "node_output", nodeOutput,
                "changes", changes,
                "improvement_score", parsedResult.get("improvementScore")
        ));
    }

    private Map<String, Object> parseOptimizeResult(String result) {
        Map<String, Object> parsed = JsonParseHelper.parseToMap(result);
        if (parsed.isEmpty()) {
            return JsonParseHelper.getDefaultOptimizeResult();
        }
        return parsed;
    }
}
