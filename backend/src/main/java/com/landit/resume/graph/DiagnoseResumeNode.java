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
 * 简历诊断节点（快速模式）
 * 调用 AI 分析简历质量
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class DiagnoseResumeNode implements AsyncNodeActionWithConfig {

    private final ChatClient.Builder chatClientBuilder;

    private static final String DIAGNOSE_PROMPT = """
            你是一位拥有15年经验的资深简历优化专家和职业规划师。

            ## 任务
            分析以下简历的质量，给出评分和改进建议。

            ## 目标岗位
            %s

            ## 简历内容
            %s

            请以JSON格式返回诊断结果，格式如下：
            {
              "overallScore": 75,
              "dimensions": {
                "format": {"score": 80, "comment": "格式规范评价"},
                "content": {"score": 70, "comment": "内容质量评价"},
                "keywords": {"score": 75, "comment": "关键词匹配评价"},
                "structure": {"score": 70, "comment": "结构逻辑评价"}
              },
              "issues": [
                {"severity": "high", "type": "missing", "content": "缺少XX内容"},
                {"severity": "medium", "type": "weak", "content": "XX描述不够具体"}
              ],
              "highlights": ["亮点1", "亮点2"],
              "quickWins": ["快速改进项1", "快速改进项2"]
            }
            """;

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        log.info("=== 开始简历诊断（快速模式）===");

        String resumeContent = state.value("resume_content").map(v -> (String) v).orElse("{}");
        String targetPosition = state.value("target_position").map(v -> (String) v).orElse("未知岗位");

        ChatClient chatClient = chatClientBuilder.build();
        String prompt = String.format(DIAGNOSE_PROMPT, targetPosition, resumeContent);

        String diagnosisResult = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        log.info("诊断完成");

        // 解析诊断结果
        Map<String, Object> parsedResult = parseDiagnosisResult(diagnosisResult);

        List<String> messages = new ArrayList<>();
        messages.add("完成简历诊断（快速模式）");

        // 构建节点输出数据（用于 SSE）
        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put("node", "diagnose_quick");
        nodeOutput.put("progress", 35);
        nodeOutput.put("message", "简历诊断完成");
        nodeOutput.put("data", parsedResult);

        return CompletableFuture.completedFuture(Map.of(
                "diagnosis_result", diagnosisResult,
                "messages", messages,
                "current_step", "diagnose_quick",
                "node_output", nodeOutput,
                "overall_score", parsedResult.get("overallScore"),
                "dimensions", parsedResult.get("dimensions"),
                "issues", parsedResult.get("issues"),
                "highlights", parsedResult.get("highlights"),
                "quick_wins", parsedResult.get("quickWins")
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
