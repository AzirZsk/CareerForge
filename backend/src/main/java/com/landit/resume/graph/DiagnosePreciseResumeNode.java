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
 * 简历诊断节点（精准模式）
 * 基于搜索结果进行精准诊断，使用 JSON Schema 约束输出格式
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class DiagnosePreciseResumeNode implements AsyncNodeActionWithConfig {

    private final ChatClient.Builder chatClientBuilder;
    private final AIPromptProperties aiPromptProperties;
    private final GraphSchemaRegistry graphSchemaRegistry;

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        log.info("=== 开始简历诊断（精准模式）===");

        String resumeContent = state.value("resume_content").map(v -> (String) v).orElse("{}");
        String targetPosition = state.value("target_position").map(v -> (String) v).orElse("未知岗位");
        String searchResults = state.value("search_results").map(v -> (String) v).orElse("暂无搜索结果");

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
                "diagnose_precise",
                50,
                "精准诊断完成（基于市场数据分析）",
                response
        );

        // 从精准分析中提取匹配分数
        Integer matchScore = null;
        if (response.getPreciseAnalysis() != null) {
            matchScore = calculateMatchScore(response.getPreciseAnalysis());
        }

        return CompletableFuture.completedFuture(Map.of(
                "diagnosis_result", diagnosisResult,
                "messages", messages,
                "current_step", "diagnose_precise",
                "node_output", nodeOutput,
                "overall_score", response.getOverallScore(),
                "match_score", matchScore,
                "dimensions", response.getDimensionScores(),
                "market_requirements", response.getPreciseAnalysis() != null ? response.getPreciseAnalysis().getMarketRequirements() : null,
                "skill_match", response.getPreciseAnalysis() != null ? response.getPreciseAnalysis().getMatchAnalysis() : null,
                "issues", response.getWeaknesses()
        ));
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
