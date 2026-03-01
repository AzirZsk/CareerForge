package com.landit.resume.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.schema.GraphSchemaRegistry;
import com.landit.common.util.ChatClientHelper;
import com.landit.common.util.JsonParseHelper;
import com.landit.resume.dto.DiagnoseResumeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 简历诊断节点（快速模式）
 * 调用 AI 分析简历质量，使用 JSON Schema 约束输出格式
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class DiagnoseResumeNode implements AsyncNodeActionWithConfig {

    private final ChatClient.Builder chatClientBuilder;
    private final AIPromptProperties aiPromptProperties;
    private final GraphSchemaRegistry graphSchemaRegistry;

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        log.info("=== 开始简历诊断（快速模式）===");

        String resumeContent = state.value("resume_content").map(v -> (String) v).orElse("{}");
        String targetPosition = state.value("target_position").map(v -> (String) v).orElse("未知岗位");

        String prompt = aiPromptProperties.getGraph().getDiagnoseQuick()
                .replace("{targetPosition}", targetPosition)
                .replace("{resumeContent}", resumeContent);

        // 使用 JSON Schema 约束调用大模型
        String diagnosisResult = ChatClientHelper.callStreamAndCollectWithSchema(
                chatClientBuilder,
                prompt,
                graphSchemaRegistry.buildDiagnosisSchema(),
                "DiagnoseResumeResponse"
        );

        log.info("诊断完成");

        // 解析诊断结果为实体
        DiagnoseResumeResponse response = JsonParseHelper.parseToEntity(diagnosisResult, DiagnoseResumeResponse.class);

        List<String> messages = new ArrayList<>();
        messages.add("完成简历诊断（快速模式）");

        // 构建节点输出数据（用于 SSE）
        Map<String, Object> nodeOutput = JsonParseHelper.buildNodeOutput(
                "diagnose_quick",
                35,
                "简历诊断完成",
                response
        );

        return CompletableFuture.completedFuture(Map.of(
                "diagnosis_result", diagnosisResult,
                "messages", messages,
                "current_step", "diagnose_quick",
                "node_output", nodeOutput,
                "overall_score", response.getOverallScore(),
                "dimensions", response.getDimensionScores(),
                "issues", response.getWeaknesses(),
                "highlights", response.getStrengths(),
                "quick_wins", response.getQuickWins()
        ));
    }
}
