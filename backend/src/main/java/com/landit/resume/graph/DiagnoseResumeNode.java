package com.landit.resume.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.util.ChatClientHelper;
import cn.hutool.core.util.StrUtil;
import com.landit.common.util.JsonParseHelper;
import com.landit.resume.dto.DiagnoseResumeResponse;
import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.landit.resume.graph.ResumeOptimizeGraphConstants.*;

/**
 * 简历诊断节点
 * 调用 AI 分析简历质量，使用 JSON Schema 约束输出格式
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
        log.info("=== 开始简历诊断===");

        ResumeDetailVO resumeDetail = (ResumeDetailVO) state.value(STATE_RESUME_CONTENT).orElse(null);
        Objects.requireNonNull(resumeDetail);
        String targetPosition = StrUtil.isNotBlank(resumeDetail.getTargetPosition())
                ? resumeDetail.getTargetPosition()
                : DEFAULT_TARGET_POSITION;

        // 使用拆分提示词调用（前缀缓存优化）
        AIPromptProperties.PromptConfig promptConfig = aiPromptProperties.getGraph().getDiagnoseQuickConfig();
        String systemPrompt = promptConfig.getSystemPrompt();
        // 获取简历 Markdown 文本
        String resumeMarkdown = resumeDetail.getMarkdownContent() != null
                ? resumeDetail.getMarkdownContent()
                : "";
        // 构建结构化的模块内容（使用简短标识符替代雪花 ID，避免 AI 幻觉）
        Map<String, String> shortIdToRealIdMap = new HashMap<>();
        String resumeSections = buildStructuredResumeContent(resumeDetail, shortIdToRealIdMap);
        String userPrompt = ChatClientHelper.renderTemplate(
                promptConfig.getUserPromptTemplate(),
                Map.of("targetPosition", targetPosition, "resumeMarkdown", resumeMarkdown, "resumeSections", resumeSections)
        );

        // 调用 AI 并自动解析（带重试）
        DiagnoseResumeResponse response = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, DiagnoseResumeResponse.class
        );

        // 将 AI 返回的简短标识符映射回原始雪花 ID
        mapShortIdsToRealIds(response, shortIdToRealIdMap);

        String diagnosisResult = JsonParseHelper.toJsonString(response);

        log.info("诊断完成");

        // 诊断完成后立即保存评分到数据库
        String resumeId = resumeDetail.getId();
        if (resumeId != null) {
            try {
                // 保存整体评分和维度评分
                resumeService.updateDiagnosisScores(resumeId, response.getOverallScore(), response.getDimensionScores());
                // 保存模块评分
                if (response.getSectionScores() != null && !response.getSectionScores().isEmpty()) {
                    resumeService.updateSectionScores(resumeId, response.getSectionScores());
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
                STATE_QUICK_WINS, response.getQuickWins()
        );
    }

    /**
     * 构建结构化的简历内容，使用简短标识符替代雪花 ID
     * 简短标识符格式：{type}_{index}，如 work_1, project_2, skills_1
     *
     * @param resumeDetail      简历详情
     * @param shortIdToRealIdMap 用于存储简短标识符到真实 ID 的映射
     * @return 结构化的内容字符串
     */
    private String buildStructuredResumeContent(ResumeDetailVO resumeDetail, Map<String, String> shortIdToRealIdMap) {
        StringBuilder sb = new StringBuilder();
        List<ResumeDetailVO.ResumeSectionVO> sections = resumeDetail.getSections();
        if (sections == null || sections.isEmpty()) {
            return "";
        }

        // 用于统计每种类型的序号
        Map<String, Integer> typeCounters = new HashMap<>();

        for (ResumeDetailVO.ResumeSectionVO section : sections) {
            String type = section.getType() != null ? section.getType().toLowerCase() : "section";
            int index = typeCounters.getOrDefault(type, 0) + 1;
            typeCounters.put(type, index);

            // 生成简短标识符：type_index（如 work_1, project_2）
            String shortId = type + "_" + index;

            // 记录映射关系
            if (section.getId() != null) {
                shortIdToRealIdMap.put(shortId, section.getId());
            }

            // 构建使用简短标识符的 JSON
            sb.append(buildSectionJsonWithShortId(section, shortId, shortIdToRealIdMap)).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * 构建单个 section 的 JSON，使用简短标识符替代原始 ID
     */
    private String buildSectionJsonWithShortId(ResumeDetailVO.ResumeSectionVO section, String shortId, Map<String, String> shortIdToRealIdMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(shortId).append("\",");
        sb.append("\"type\":\"").append(section.getType()).append("\"");

        if (section.getTitle() != null) {
            sb.append(",\"title\":\"").append(escapeJson(section.getTitle())).append("\"");
        }

        // 处理聚合类型的 items
        if (section.getItems() != null && !section.getItems().isEmpty()) {
            sb.append(",\"items\":[");
            for (int i = 0; i < section.getItems().size(); i++) {
                ResumeDetailVO.ResumeSectionItemVO item = section.getItems().get(i);
                if (i > 0) sb.append(",");
                // 为 item 也生成简短标识符：shortId_itemIndex（如 work_1_1, work_1_2）
                String itemShortId = shortId + "_" + (i + 1);
                if (item.getId() != null) {
                    shortIdToRealIdMap.put(itemShortId, item.getId());
                }
                sb.append("{\"id\":\"").append(itemShortId).append("\"");
                if (item.getTitle() != null) {
                    sb.append(",\"title\":\"").append(escapeJson(item.getTitle())).append("\"");
                }
                if (item.getContent() != null) {
                    sb.append(",\"content\":").append(JsonParseHelper.toJsonString(item.getContent()));
                }
                sb.append("}");
            }
            sb.append("]");
        } else if (section.getContent() != null) {
            // 单条类型，直接输出 content
            sb.append(",\"content\":").append(JsonParseHelper.toJsonString(section.getContent()));
        }

        sb.append("}");
        return sb.toString();
    }

    /**
     * 转义 JSON 字符串中的特殊字符
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 将 AI 返回的简短标识符映射回原始雪花 ID
     *
     * @param response            AI 响应
     * @param shortIdToRealIdMap  简短标识符到真实 ID 的映射
     */
    private void mapShortIdsToRealIds(DiagnoseResumeResponse response, Map<String, String> shortIdToRealIdMap) {
        if (response.getSectionScores() == null || shortIdToRealIdMap.isEmpty()) {
            return;
        }

        Map<String, Integer> originalScores = response.getSectionScores();
        Map<String, Integer> mappedScores = new HashMap<>();

        for (Map.Entry<String, Integer> entry : originalScores.entrySet()) {
            String shortId = entry.getKey();
            String realId = shortIdToRealIdMap.get(shortId);
            if (realId != null) {
                mappedScores.put(realId, entry.getValue());
            } else {
                log.warn("无法映射 sectionScore id: {}, 该简短标识符不在映射表中", shortId);
            }
        }

        response.setSectionScores(mappedScores);
        log.info("sectionScores 映射完成: 原始 {} 条 -> 映射 {} 条", originalScores.size(), mappedScores.size());
    }
}
