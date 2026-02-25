package com.landit.resume.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 人工审核节点
 * 等待人工审核优化后的简历内容
 *
 * 注意：此节点配置了 interruptBefore，会在执行前中断等待人工输入
 *
 * @author Azir
 */
@Slf4j
public class HumanReviewNode implements AsyncNodeActionWithConfig {

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        log.info("=== 人工审核 ===");

        String optimizedSections = state.value("optimized_sections").map(v -> (String) v).orElse("{}");
        String suggestions = state.value("suggestions").map(v -> (String) v).orElse("[]");

        // 检查是否已审核通过
        Boolean approved = state.value("approved").map(v -> (Boolean) v).orElse(false);

        if (!approved) {
            log.info("等待人工审核...");
            List<String> messages = new ArrayList<>();
            messages.add("等待人工审核...");

            // 构建节点输出数据（用于 SSE）
            Map<String, Object> nodeOutput = new HashMap<>();
            nodeOutput.put("node", "human_review");
            nodeOutput.put("progress", 90);
            nodeOutput.put("message", "等待人工审核确认");
            nodeOutput.put("data", Map.of(
                    "status", "waiting_for_review",
                    "optimizedContent", optimizedSections
            ));

            return CompletableFuture.completedFuture(Map.of(
                    "status", "waiting_for_review",
                    "messages", messages,
                    "current_step", "human_review",
                    "node_output", nodeOutput
            ));
        }

        log.info("人工审核通过");
        List<String> messages = new ArrayList<>();
        messages.add("人工审核通过");

        // 构建节点输出数据（用于 SSE）
        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put("node", "human_review");
        nodeOutput.put("progress", 90);
        nodeOutput.put("message", "人工审核已通过");
        nodeOutput.put("data", Map.of("status", "approved"));

        return CompletableFuture.completedFuture(Map.of(
                "status", "approved",
                "messages", messages,
                "current_step", "human_review",
                "node_output", nodeOutput
        ));
    }
}
