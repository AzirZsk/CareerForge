package com.landit.resume.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.landit.common.util.JsonParseHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 解析简历节点
 * 从状态中获取简历内容，准备后续处理
 *
 * @author Azir
 */
@Slf4j
public class ParseResumeNode implements AsyncNodeActionWithConfig {

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        log.info("=== 开始解析简历 ===");

        String resumeId = state.value("resume_id").map(v -> (String) v).orElse("");
        String resumeContent = state.value("resume_content").map(v -> (String) v).orElse("{}");
        String targetPosition = state.value("target_position").map(v -> (String) v).orElse("未知岗位");

        log.info("简历ID: {}, 目标岗位: {}", resumeId, targetPosition);

        // 解析简历模块信息
        Map<String, Object> parseResult = parseResumeModules(resumeContent);

        List<String> messages = new ArrayList<>();
        messages.add("开始解析简历...");
        messages.add("简历ID: " + resumeId);
        messages.add("目标岗位: " + targetPosition);

        // 构建节点输出数据（用于 SSE）
        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put("node", "parse_resume");
        nodeOutput.put("progress", 15);
        nodeOutput.put("message", "简历解析完成");
        nodeOutput.put("data", Map.of(
                "resumeId", resumeId,
                "targetPosition", targetPosition,
                "modules", parseResult.get("modules"),
                "completeness", parseResult.get("completeness"),
                "moduleCount", parseResult.get("moduleCount")
        ));

        return CompletableFuture.completedFuture(Map.of(
                "messages", messages,
                "current_step", "parse_resume",
                "node_output", nodeOutput,
                "modules", parseResult.get("modules"),
                "completeness", parseResult.get("completeness")
        ));
    }

    /**
     * 解析简历模块信息
     */
    private Map<String, Object> parseResumeModules(String resumeContent) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> modules = new ArrayList<>();

        try {
            Map<String, Object> content = JsonParseHelper.parseToMap(resumeContent);

            // 解析 sections
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sections = (List<Map<String, Object>>) content.get("sections");
            if (sections != null) {
                for (Map<String, Object> section : sections) {
                    Map<String, Object> moduleInfo = new HashMap<>();
                    moduleInfo.put("id", section.get("id"));
                    moduleInfo.put("type", section.get("type"));
                    moduleInfo.put("title", section.get("title"));
                    moduleInfo.put("score", section.get("score"));
                    moduleInfo.put("hasContent", section.get("content") != null);
                    modules.add(moduleInfo);
                }
            }

            int totalModules = modules.size();
            int completeModules = (int) modules.stream()
                    .filter(m -> Boolean.TRUE.equals(m.get("hasContent")))
                    .count();

            result.put("modules", modules);
            result.put("moduleCount", totalModules);
            result.put("completeness", totalModules > 0 ? (completeModules * 100 / totalModules) : 0);

        } catch (Exception e) {
            log.error("解析简历模块失败", e);
            result.put("modules", modules);
            result.put("moduleCount", 0);
            result.put("completeness", 0);
        }

        return result;
    }
}
