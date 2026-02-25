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
 * 简历诊断节点（精准模式）
 * 基于搜索结果进行精准诊断
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class DiagnosePreciseResumeNode implements AsyncNodeActionWithConfig {

    private final ChatClient.Builder chatClientBuilder;

    private static final String DIAGNOSE_PRECISE_PROMPT = """
            你是一位拥有15年经验的资深简历优化专家。

            ## 任务
            基于最新的市场岗位要求，分析简历与目标岗位的匹配度。

            ## 目标岗位
            %s

            ## 搜索结果（2025年该岗位技能要求）
            %s

            ## 简历内容
            %s

            请以JSON格式返回诊断结果，格式如下：
            {
              "overallScore": 75,
              "matchScore": 70,
              "dimensions": {
                "format": {"score": 80, "comment": "格式规范评价"},
                "content": {"score": 70, "comment": "内容质量评价"},
                "keywords": {"score": 75, "comment": "关键词匹配评价"},
                "structure": {"score": 70, "comment": "结构逻辑评价"}
              },
              "marketRequirements": {
                "required": ["技能1", "技能2"],
                "preferred": ["技能3", "技能4"],
                "trending": ["热门技能1"]
              },
              "skillMatch": {
                "matched": ["已匹配技能"],
                "missing": ["缺失技能"],
                "partial": ["部分匹配技能"]
              },
              "issues": [
                {"severity": "high", "type": "missing", "content": "缺少XX技能"}
              ],
              "suggestions": ["建议1", "建议2"]
            }
            """;

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        log.info("=== 开始简历诊断（精准模式）===");

        String resumeContent = state.value("resume_content").map(v -> (String) v).orElse("{}");
        String targetPosition = state.value("target_position").map(v -> (String) v).orElse("未知岗位");
        String searchResults = state.value("search_results").map(v -> (String) v).orElse("暂无搜索结果");

        ChatClient chatClient = chatClientBuilder.build();
        String prompt = String.format(DIAGNOSE_PRECISE_PROMPT, targetPosition, searchResults, resumeContent);

        String diagnosisResult = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

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
