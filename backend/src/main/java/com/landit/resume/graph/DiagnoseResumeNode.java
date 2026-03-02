package com.landit.resume.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
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

import static com.landit.resume.graph.ResumeOptimizeGraphConstants.*;

/**
 * 简历诊断节点（快速模式）
 * 调用 AI 分析简历质量，使用 JSON Schema 约束输出格式
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class DiagnoseResumeNode implements NodeAction {

    private final ChatClient chatClient;
    private final AIPromptProperties aiPromptProperties;
    private final GraphSchemaRegistry graphSchemaRegistry;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始简历诊断（快速模式）===");

        String resumeContent = state.value(STATE_RESUME_CONTENT).map(v -> (String) v).orElse(DEFAULT_EMPTY_JSON);
        String targetPosition = state.value(STATE_TARGET_POSITION).map(v -> (String) v).orElse(DEFAULT_TARGET_POSITION);

        String prompt = aiPromptProperties.getGraph().getDiagnoseQuick()
                .replace("{targetPosition}", targetPosition)
                .replace("{resumeContent}", resumeContent);

        // 使用 JSON Schema 约束调用大模型
        String diagnosisResult = ChatClientHelper.callStreamAndCollectWithSchema(
                chatClient,
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
                NODE_DIAGNOSE_QUICK,
                35,
                "简历诊断完成",
                response
        );

        return Map.of(
                STATE_DIAGNOSIS_RESULT, diagnosisResult,
                STATE_MESSAGES, messages,
                STATE_CURRENT_STEP, NODE_DIAGNOSE_QUICK,
                STATE_NODE_OUTPUT, nodeOutput,
                STATE_OVERALL_SCORE, response.getOverallScore(),
                STATE_DIMENSIONS, response.getDimensionScores(),
                STATE_ISSUES, response.getWeaknesses(),
                STATE_HIGHLIGHTS, response.getStrengths(),
                STATE_QUICK_WINS, response.getQuickWins()
        );
    }
}
