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
 * 保存版本节点
 * 保存优化后的简历版本
 *
 * @author Azir
 */
@Slf4j
public class SaveVersionNode implements AsyncNodeActionWithConfig {

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        log.info("=== 保存版本 ===");

        String resumeId = state.value("resume_id").map(v -> (String) v).orElse("");
        String optimizedSections = state.value("optimized_sections").map(v -> (String) v).orElse("{}");

        // 获取优化前后的分数
        Integer improvementScore = state.value("improvement_score").map(v -> (Integer) v).orElse(0);
        Integer originalScore = state.value("overall_score").map(v -> (Integer) v).orElse(0);
        Integer newScore = originalScore + improvementScore;

        // TODO: 调用 ResumeService 保存版本
        log.info("保存简历版本: resumeId={}", resumeId);

        // 模拟版本号
        String versionId = "v" + System.currentTimeMillis();
        String versionName = "优化版本_" + new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());

        List<String> messages = new ArrayList<>();
        messages.add("简历版本保存成功");
        messages.add("优化工作流完成");

        // 构建节点输出数据（用于 SSE）
        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put("node", "save_version");
        nodeOutput.put("progress", 100);
        nodeOutput.put("message", "优化完成！简历已保存");
        nodeOutput.put("data", Map.of(
                "status", "completed",
                "resumeId", resumeId,
                "versionId", versionId,
                "versionName", versionName,
                "originalScore", originalScore,
                "newScore", newScore,
                "improvementScore", improvementScore
        ));

        return CompletableFuture.completedFuture(Map.of(
                "status", "completed",
                "messages", messages,
                "current_step", "save_version",
                "node_output", nodeOutput,
                "version_id", versionId,
                "version_name", versionName
        ));
    }
}
