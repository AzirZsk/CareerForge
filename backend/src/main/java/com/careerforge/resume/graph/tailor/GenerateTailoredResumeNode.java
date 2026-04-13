package com.careerforge.resume.graph.tailor;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.careerforge.common.config.AIPromptProperties;
import com.careerforge.common.util.ChatClientHelper;
import com.careerforge.common.util.JsonParseHelper;
import com.careerforge.resume.dto.JobRequirements;
import com.careerforge.resume.dto.MatchAnalysis;
import com.careerforge.resume.dto.ResumeDetailVO;
import com.careerforge.resume.dto.TailorResumeResponse;
import com.careerforge.resume.util.TailoredResumeToSectionConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.careerforge.resume.graph.tailor.TailorResumeGraphConstants.*;

/**
 * 生成定制简历节点
 * 根据 JD 和匹配分析生成定制简历
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class GenerateTailoredResumeNode implements NodeAction {

    private final ChatClient chatClient;
    private final AIPromptProperties aiPromptProperties;
    private final TailoredResumeToSectionConverter sectionConverter;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始生成定制简历 ===");

        ResumeDetailVO resumeDetail = (ResumeDetailVO) state.value(STATE_RESUME_CONTENT).orElse(null);
        Objects.requireNonNull(resumeDetail, "简历内容不能为空");

        String jobRequirementsJson = (String) state.value(STATE_JOB_REQUIREMENTS).orElse("{}");
        String matchAnalysisJson = (String) state.value(STATE_MATCH_ANALYSIS).orElse("{}");
        String targetPosition = (String) state.value(STATE_TARGET_POSITION).orElse("");

        // 获取简历结构化内容（仅传入 sections，避免无关字段浪费 token）
        String resumeContent = JsonParseHelper.toJsonString(
                Map.of(
                        "targetPosition", targetPosition,
                        "sections", resumeDetail.getSections() != null ? resumeDetail.getSections() : List.of()
                )
        );

        // 使用拆分提示词调用
        AIPromptProperties.PromptConfig promptConfig = aiPromptProperties.getTailorGraph().getGenerateTailoredConfig();
        String systemPrompt = promptConfig.getSystemPrompt();
        String userPrompt = ChatClientHelper.renderTemplate(
                promptConfig.getUserPromptTemplate(),
                Map.of(
                        "targetPosition", targetPosition,
                        "resumeContent", resumeContent,
                        "jobRequirements", jobRequirementsJson,
                        "matchAnalysis", matchAnalysisJson
                )
        );

        // 调用 AI 并自动解析
        TailorResumeResponse response = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, TailorResumeResponse.class
        );

        String tailoredResume = JsonParseHelper.toJsonString(response);

        log.info("定制简历生成完成，调整说明: {} 条", response.getTailorNotes() != null ? response.getTailorNotes().size() : 0);

        List<String> messages = new ArrayList<>();
        messages.add("完成定制简历生成");

        // ===== 构建对比数据 =====

        // 1. 获取原始简历区块（beforeSection）
        List<ResumeDetailVO.ResumeSectionVO> beforeSection = resumeDetail.getSections();

        // 2. 将 TailorResumeResponse 转换为 ResumeSectionVO 列表（afterSection）
        List<ResumeDetailVO.ResumeSectionVO> afterSection = sectionConverter.convert(response, resumeDetail.getId());

        // 3. 从匹配分析获取匹配分数
        MatchAnalysis matchAnalysis = JsonParseHelper.parseToEntity(matchAnalysisJson, MatchAnalysis.class);
        int matchScore = matchAnalysis != null ? matchAnalysis.getMatchScore() : 0;

        // 4. 计算整体提升分数（匹配分数 + 相关性评分的平均值）
        int improvementScore = calculateImprovementScore(response, matchScore);

        // 5. 构建节点输出数据（用于 SSE）
        Map<String, Object> nodeOutput = JsonParseHelper.buildNodeOutput(
                NODE_GENERATE_TAILORED,
                100,
                "定制简历生成完成",
                Map.of(
                        "beforeSection", beforeSection,
                        "afterSection", afterSection,
                        "improvementScore", improvementScore,
                        "matchScore", matchScore,
                        "tailorNotes", response.getTailorNotes(),
                        "sectionRelevanceScores", response.getSectionRelevanceScores(),
                        "dimensionScores", response.getDimensionScores()
                )
        );

        return Map.of(
                STATE_TAILORED_RESUME, tailoredResume,
                STATE_TAILORED_SECTIONS, tailoredResume,
                STATE_BEFORE_SECTION, beforeSection,
                STATE_AFTER_SECTION, afterSection,
                STATE_IMPROVEMENT_SCORE, improvementScore,
                STATE_MESSAGES, messages,
                STATE_CURRENT_STEP, NODE_GENERATE_TAILORED,
                STATE_NODE_OUTPUT, nodeOutput
        );
    }

    /**
     * 计算整体提升分数
     * 综合匹配分数和各区块相关性评分
     */
    private int calculateImprovementScore(TailorResumeResponse response, int matchScore) {
        Map<String, Integer> scores = response.getSectionRelevanceScores();
        if (scores == null || scores.isEmpty()) {
            return matchScore;
        }

        double avgRelevance = scores.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(matchScore);

        return (int) Math.round((matchScore + avgRelevance) / 2.0);
    }
}
