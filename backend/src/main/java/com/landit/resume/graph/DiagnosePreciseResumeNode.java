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
 * 简历诊断节点（精准模式）
 * 基于搜索结果进行精准诊断，使用 JSON Schema 约束输出格式
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class DiagnosePreciseResumeNode implements NodeAction {

    private final ChatClient.Builder chatClientBuilder;
    private final AIPromptProperties aiPromptProperties;
    private final GraphSchemaRegistry graphSchemaRegistry;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始简历诊断（精准模式）===");

        String resumeContent = state.value(STATE_RESUME_CONTENT).map(v -> (String) v).orElse(DEFAULT_EMPTY_JSON);
        String targetPosition = state.value(STATE_TARGET_POSITION).map(v -> (String) v).orElse(DEFAULT_TARGET_POSITION);
        String searchResults = state.value(STATE_SEARCH_RESULTS).map(v -> (String) v).orElse(DEFAULT_SEARCH_RESULTS);

        String prompt = aiPromptProperties.getGraph().getDiagnosePrecise()
                .replace("{targetPosition}", targetPosition)
                .replace("{searchResults}", searchResults)
                .replace("{resumeContent}", resumeContent);

        // 使用 JSON Schema 约束调用大模型
        String diagnosisResult = ChatClientHelper.callStreamAndCollectWithSchema(
                chatClientBuilder,
                prompt,
                graphSchemaRegistry.buildDiagnosisSchema(),
                "DiagnoseResumeResponse"
        );

        log.info("精准诊断完成");

        // 解析诊断结果为实体
        DiagnoseResumeResponse response = JsonParseHelper.parseToEntity(diagnosisResult, DiagnoseResumeResponse.class);

        List<String> messages = new ArrayList<>();
        messages.add("完成简历诊断（精准模式，基于市场数据）");

        // 构建节点输出数据（用于 SSE）
        Map<String, Object> nodeOutput = JsonParseHelper.buildNodeOutput(
                NODE_DIAGNOSE_PRECISE,
                50,
                "精准诊断完成（基于市场数据分析）",
                response
        );

        // 从精准分析中提取匹配分数
        Integer matchScore = null;
        if (response.getPreciseAnalysis() != null) {
            matchScore = calculateMatchScore(response.getPreciseAnalysis());
        }

        return Map.of(
                STATE_DIAGNOSIS_RESULT, diagnosisResult,
                STATE_MESSAGES, messages,
                STATE_CURRENT_STEP, NODE_DIAGNOSE_PRECISE,
                STATE_NODE_OUTPUT, nodeOutput,
                STATE_OVERALL_SCORE, response.getOverallScore(),
                STATE_MATCH_SCORE, matchScore,
                STATE_DIMENSIONS, response.getDimensionScores(),
                STATE_MARKET_REQUIREMENTS, response.getPreciseAnalysis() != null ? response.getPreciseAnalysis().getMarketRequirements() : null,
                STATE_SKILL_MATCH, response.getPreciseAnalysis() != null ? response.getPreciseAnalysis().getMatchAnalysis() : null,
                STATE_ISSUES, response.getWeaknesses()
        );
    }

    /**
     * 计算匹配分数
     */
    private Integer calculateMatchScore(DiagnoseResumeResponse.PreciseAnalysis preciseAnalysis) {
        DiagnoseResumeResponse.MatchAnalysis matchAnalysis = preciseAnalysis.getMatchAnalysis();
        if (matchAnalysis == null) {
            return 0;
        }
        int matched = matchAnalysis.getMatched() != null ? matchAnalysis.getMatched().size() : 0;
        int missing = matchAnalysis.getMissing() != null ? matchAnalysis.getMissing().size() : 0;
        int partial = matchAnalysis.getPartialMatch() != null ? matchAnalysis.getPartialMatch().size() : 0;
        int total = matched + missing + partial;
        if (total == 0) {
            return 0;
        }
        // 匹配权重100%，部分匹配权重50%
        return (int) ((matched + partial * 0.5) / total * 100);
    }
}
