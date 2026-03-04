package com.landit.resume.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.util.ChatClientHelper;
import com.landit.common.util.JsonParseHelper;
import com.landit.resume.dto.DiagnoseResumeResponse;
import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.landit.resume.graph.ResumeOptimizeGraphConstants.*;

/**
 * 简历诊断节点（快速模式）
 * 调用 AI 分析简历质量，使用 JSON Schema 约束输出格式
 * 使用拆分提示词方式调用以利用前缀缓存优化
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class DiagnoseResumeNode implements NodeAction {

    private final ChatClient chatClient;
    private final AIPromptProperties aiPromptProperties;
    private final ResumeService resumeService;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始简历诊断（快速模式）===");

        ResumeDetailVO resumeDetail = (ResumeDetailVO) state.value(STATE_RESUME_CONTENT).orElse(null);
        Objects.requireNonNull(resumeDetail);
        String targetPosition = resumeDetail.getTargetPosition() != null
                ? resumeDetail.getTargetPosition()
                : DEFAULT_TARGET_POSITION;

        // 使用拆分提示词调用（前缀缓存优化）
        AIPromptProperties.PromptConfig promptConfig = aiPromptProperties.getGraph().getDiagnoseQuickConfig();
        String systemPrompt = promptConfig.getSystemPrompt();
        String userPrompt = ChatClientHelper.renderTemplate(
                promptConfig.getUserPromptTemplate(),
                Map.of("targetPosition", targetPosition, "resumeMarkdown", resumeDetail.getMarkdownContent())
        );

        // 调用 AI 并自动解析（带重试）
        DiagnoseResumeResponse response = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, DiagnoseResumeResponse.class
        );
        String diagnosisResult = JsonParseHelper.toJsonString(response);

        log.info("诊断完成");

        // 诊断完成后立即保存评分到数据库
        String resumeId = resumeDetail.getId();
        if (resumeId != null) {
            try {
                resumeService.updateDiagnosisScores(resumeId, response.getOverallScore(), response.getDimensionScores());
                log.info("诊断评分已保存: resumeId={}, overallScore={}", resumeId, response.getOverallScore());
            } catch (Exception e) {
                log.error("保存诊断评分失败: resumeId={}", resumeId, e);
            }
        }

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
