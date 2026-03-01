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

import static com.landit.resume.graph.ResumeOptimizeGraphConstants.*;

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

        String optimizedSections = state.value(STATE_OPTIMIZED_SECTIONS).map(v -> (String) v).orElse(DEFAULT_EMPTY_JSON);
        String suggestions = state.value(STATE_SUGGESTIONS).map(v -> (String) v).orElse(DEFAULT_EMPTY_ARRAY);

        // 检查是否已审核通过
        Boolean approved = state.value(STATE_APPROVED).map(v -> (Boolean) v).orElse(false);

        if (!approved) {
            log.info("等待人工审核...");
            List<String> messages = new ArrayList<>();
            messages.add("等待人工审核...");

            // 构建节点输出数据（用于 SSE）
            Map<String, Object> nodeOutput = new HashMap<>();
            nodeOutput.put(OUTPUT_NODE, NODE_HUMAN_REVIEW);
            nodeOutput.put(OUTPUT_PROGRESS, 90);
            nodeOutput.put(OUTPUT_MESSAGE, "等待人工审核确认");
            nodeOutput.put(OUTPUT_DATA, Map.of(
                    STATE_STATUS, STATUS_WAITING_FOR_REVIEW,
                    "optimizedContent", optimizedSections
            ));

            return CompletableFuture.completedFuture(Map.of(
                    STATE_STATUS, STATUS_WAITING_FOR_REVIEW,
                    STATE_MESSAGES, messages,
                    STATE_CURRENT_STEP, NODE_HUMAN_REVIEW,
                    STATE_NODE_OUTPUT, nodeOutput
            ));
        }

        log.info("人工审核通过");
        List<String> messages = new ArrayList<>();
        messages.add("人工审核通过");

        // 构建节点输出数据（用于 SSE）
        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put(OUTPUT_NODE, NODE_HUMAN_REVIEW);
        nodeOutput.put(OUTPUT_PROGRESS, 90);
        nodeOutput.put(OUTPUT_MESSAGE, "人工审核已通过");
        nodeOutput.put(OUTPUT_DATA, Map.of(STATE_STATUS, STATUS_APPROVED));

        return CompletableFuture.completedFuture(Map.of(
                STATE_STATUS, STATUS_APPROVED,
                STATE_MESSAGES, messages,
                STATE_CURRENT_STEP, NODE_HUMAN_REVIEW,
                STATE_NODE_OUTPUT, nodeOutput
        ));
    }
}
