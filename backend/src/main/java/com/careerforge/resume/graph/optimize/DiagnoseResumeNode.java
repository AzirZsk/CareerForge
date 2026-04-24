package com.careerforge.resume.graph.optimize;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.careerforge.common.config.prompt.ResumeOptimizePromptProperties;
import com.careerforge.common.config.prompt.PromptConfig;
import com.careerforge.common.util.ChatClientHelper;
import cn.hutool.core.util.StrUtil;
import com.careerforge.common.util.JsonParseHelper;
import com.careerforge.resume.dto.DiagnoseResumeResponse;
import com.careerforge.resume.dto.ResumeDetailVO;
import com.careerforge.resume.service.ResumeService;
import com.careerforge.resume.util.ResumeSectionShortener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.careerforge.resume.graph.optimize.ResumeOptimizeGraphConstants.*;

/**
 * 简历诊断节点
 * 调用 AI 分析简历质量，使用 JSON Schema 约束输出格式
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiagnoseResumeNode implements NodeAction {

    private final ChatClient chatClient;
    private final ResumeOptimizePromptProperties promptProperties;
    private final ResumeService resumeService;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始简历诊断===");

        ResumeDetailVO resumeDetail = (ResumeDetailVO) state.value(STATE_RESUME_CONTENT).orElse(null);
        Objects.requireNonNull(resumeDetail);
        String targetPosition = StrUtil.isNotBlank(resumeDetail.getTargetPosition())
                ? resumeDetail.getTargetPosition()
                : DEFAULT_TARGET_POSITION;

        // 使用拆分提示词调用（前缀缓存优化）
        PromptConfig promptConfig = promptProperties.getDiagnoseQuickConfig();
        String systemPrompt = promptConfig.getSystemPrompt();
        // 获取简历 Markdown 文本
        String resumeMarkdown = resumeDetail.getMarkdownContent() != null
                ? resumeDetail.getMarkdownContent()
                : "";
        // 构建结构化的模块内容（使用简短标识符替代雪花 ID，避免 AI 幻觉）
        ResumeSectionShortener.ShortenResult shortenResult = ResumeSectionShortener.shorten(resumeDetail);
        String resumeSections = shortenResult.getShortenedContent();
        Map<String, String> shortIdToRealIdMap = shortenResult.getShortIdToRealIdMap();
        String userPrompt = ChatClientHelper.renderTemplate(
                promptConfig.getUserPromptTemplate(),
                Map.of("targetPosition", targetPosition, "resumeMarkdown", resumeMarkdown, "resumeSections", resumeSections)
        );

        // 调用 AI 并自动解析（带重试）
        DiagnoseResumeResponse response = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, DiagnoseResumeResponse.class
        );

        // 将 AI 返回的简短标识符映射回原始雪花 ID，并区分普通 section 和 custom item
        Map<String, Map<String, Integer>> mappedScores = mapShortIdsToRealIds(response, shortIdToRealIdMap);
        Map<String, Integer> normalSectionScores = mappedScores.get("normalSectionScores");
        Map<String, Integer> customItemScores = mappedScores.get("customItemScores");

        String diagnosisResult = JsonParseHelper.toJsonString(response);

        log.info("诊断完成");

        // 诊断完成后立即保存评分到数据库
        String resumeId = resumeDetail.getId();
        if (resumeId != null) {
            try {
                // 保存整体评分和维度评分
                resumeService.updateDiagnosisScores(resumeId, response.getOverallScore(), response.getDimensionScores());
                // 保存普通模块评分
                if (normalSectionScores != null && !normalSectionScores.isEmpty()) {
                    resumeService.updateSectionScores(resumeId, normalSectionScores);
                }
                // 保存自定义区块 item 评分
                if (customItemScores != null && !customItemScores.isEmpty()) {
                    resumeService.updateCustomItemScores(resumeId, customItemScores);
                }
                log.info("诊断评分已保存: resumeId={}, overallScore={}", resumeId, response.getOverallScore());
            } catch (Exception e) {
                log.error("保存诊断评分失败: resumeId={}", resumeId, e);
            }
        }

        List<String> messages = new ArrayList<>();
        messages.add("完成简历诊断");

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
                STATE_SECTION_ID_MAP, shortIdToRealIdMap
        );
    }

    /**
     * 将 AI 返回的简短标识符映射回原始雪花 ID
     * 同时区分普通 section 评分和 custom item 评分
     *
     * @param response            AI 响应
     * @param shortIdToRealIdMap  简短标识符到真实 ID 的映射
     * @return 包含两种评分的 Map：normalSectionScores 和 customItemScores
     */
    private Map<String, Map<String, Integer>> mapShortIdsToRealIds(
            DiagnoseResumeResponse response, Map<String, String> shortIdToRealIdMap) {
        Map<String, Map<String, Integer>> result = new HashMap<>();
        Map<String, Integer> normalSectionScores = new HashMap<>();
        Map<String, Integer> customItemScores = new HashMap<>();

        if (response.getSectionScores() == null || shortIdToRealIdMap.isEmpty()) {
            result.put("normalSectionScores", normalSectionScores);
            result.put("customItemScores", customItemScores);
            return result;
        }

        Map<String, Integer> originalScores = response.getSectionScores();

        for (Map.Entry<String, Integer> entry : originalScores.entrySet()) {
            String shortId = entry.getKey();
            String realId = shortIdToRealIdMap.get(shortId);
            if (realId != null) {
                if (realId.contains(":")) {
                    // Custom item 格式：sectionId:itemIndex
                    customItemScores.put(realId, entry.getValue());
                } else {
                    // 普通 section
                    normalSectionScores.put(realId, entry.getValue());
                }
            } else {
                log.warn("无法映射 sectionScore id: {}, 该简短标识符不在映射表中", shortId);
            }
        }

        // 更新 response 的 sectionScores 为普通 section 评分（兼容后续逻辑）
        response.setSectionScores(normalSectionScores);
        log.info("sectionScores 映射完成: 普通 section {} 条, custom item {} 条",
                normalSectionScores.size(), customItemScores.size());

        result.put("normalSectionScores", normalSectionScores);
        result.put("customItemScores", customItemScores);
        return result;
    }
}
