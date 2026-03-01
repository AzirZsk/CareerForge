package com.landit.resume.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.schema.GraphSchemaRegistry;
import com.landit.common.util.ChatClientHelper;
import com.landit.common.util.JsonParseHelper;
import com.landit.resume.dto.OptimizeSectionResponse;
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
 * 对选定的简历模块进行 AI 优化，使用 JSON Schema 约束输出格式
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class OptimizeSectionNode implements AsyncNodeActionWithConfig {

    private final ChatClient.Builder chatClientBuilder;
    private final AIPromptProperties aiPromptProperties;
    private final GraphSchemaRegistry graphSchemaRegistry;

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        log.info("=== 模块内容优化 ===");

        String resumeContent = state.value("resume_content").map(v -> (String) v).orElse("{}");
        String targetPosition = state.value("target_position").map(v -> (String) v).orElse("未知岗位");
        String suggestions = state.value("suggestions").map(v -> (String) v).orElse("[]");

        // 获取用户选择的要优化的模块（默认优化全部）
        String sectionType = state.value("section_to_optimize").map(v -> (String) v).orElse("all");

        String prompt = aiPromptProperties.getGraph().getOptimizeSection()
                .replace("{sectionType}", sectionType)
                .replace("{targetPosition}", targetPosition)
                .replace("{resumeContent}", resumeContent)
                .replace("{suggestions}", suggestions);

        // 使用 JSON Schema 约束调用大模型
        String optimizeResult = ChatClientHelper.callStreamAndCollectWithSchema(
                chatClientBuilder,
                prompt,
                graphSchemaRegistry.buildOptimizeSectionSchema(),
                "OptimizeSectionResponse"
        );

        log.info("模块优化完成");

        // 解析优化结果为实体
        OptimizeSectionResponse response = JsonParseHelper.parseToEntity(optimizeResult, OptimizeSectionResponse.class);

        List<?> changes = response.getChanges() != null ? response.getChanges() : List.of();
        int changeCount = changes.size();

        List<String> messages = new ArrayList<>();
        messages.add("模块内容优化完成");
        messages.add("共优化 " + changeCount + " 处");

        // 根据优化结果的置信度决定是否需要人工审核
        String confidence = response.getConfidence() != null ? response.getConfidence() : "medium";
        boolean needsReview = !"high".equals(confidence);

        // 构建节点输出数据（用于 SSE）
        Map<String, Object> nodeOutput = JsonParseHelper.buildNodeOutput(
                "optimize_section",
                80,
                "内容优化完成，共 " + changeCount + " 处变更",
                Map.of(
                        "changes", changes,
                        "improvementScore", response.getImprovementScore(),
                        "tips", response.getTips(),
                        "confidence", confidence,
                        "needsReview", needsReview,
                        "changeCount", changeCount
                )
        );

        return CompletableFuture.completedFuture(Map.of(
                "optimized_sections", optimizeResult,
                "needs_review", needsReview,
                "messages", messages,
                "current_step", "optimize_section",
                "node_output", nodeOutput,
                "changes", changes,
                "improvement_score", response.getImprovementScore()
        ));
    }
}
