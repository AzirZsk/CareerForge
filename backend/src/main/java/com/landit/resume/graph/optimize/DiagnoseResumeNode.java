package com.landit.resume.graph.optimize;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.util.ChatClientHelper;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.landit.common.util.JsonParseHelper;
import com.landit.common.enums.SectionType;
import com.landit.resume.dto.DiagnoseResumeResponse;
import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.dto.ResumeStructuredData;
import com.landit.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.landit.resume.graph.optimize.ResumeOptimizeGraphConstants.*;

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
                STATE_QUICK_WINS, response.getQuickWins(),
                STATE_SECTION_ID_MAP, shortIdToRealIdMap
        );
    }

    /**
     * 构建结构化的简历内容，使用简短标识符替代雪花 ID
     * 简短标识符格式：{type}_{index}，如 work_1, project_2, skills_1
     * 对于 CUSTOM 类型，展开每个 item 作为独立评分单元：custom_1, custom_2, ...
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
        // 用于统计 custom item 的序号
        int customIndex = 0;

        for (ResumeDetailVO.ResumeSectionVO section : sections) {
            String type = section.getType() != null ? section.getType() : "section";

            // CUSTOM 类型特殊处理：每个 CUSTOM 区块独立一条记录， content 直接是 items 数组
            if (SectionType.CUSTOM.getCode().equals(type)) {
                // content 直接是 items 数组
                List<ResumeStructuredData.ContentItem> items = JsonParseHelper.parseToEntity(
                    section.getContent(),
                    new TypeReference<List<ResumeStructuredData.ContentItem>>() {}
                );
                if (items != null && !items.isEmpty()) {
                    // 每个 CUSTOM 区块独立一条记录，直接使用 section.id
                    String shortId = "custom_" + customIndex++;
                    // 映射格式：sectionId（当前区块独立，无需 itemIndex）
                    shortIdToRealIdMap.put(shortId, section.getId());
                    // title 从 section.getTitle() 获取
                    String title = section.getTitle();
                    // 构建单个 custom item 的 JSON
                    Map<String, Object> jsonMap = new LinkedHashMap<>();
                    jsonMap.put("id", shortId);
                    jsonMap.put("type", "CUSTOM");
                    jsonMap.put("title", title);
                    jsonMap.put("content", items);
                    sb.append(JsonParseHelper.toJsonString(jsonMap)).append("\n\n");
                }
            } else {
                // 其他类型按 section 级别处理
                int index = typeCounters.getOrDefault(type.toLowerCase(), 0) + 1;
                typeCounters.put(type.toLowerCase(), index);

                // 生成简短标识符：type_index（如 work_1, project_2）
                String shortId = type.toLowerCase() + "_" + index;

                // 记录映射关系
                if (section.getId() != null) {
                    shortIdToRealIdMap.put(shortId, section.getId());
                }

                // 构建使用简短标识符的 JSON
                sb.append(buildSectionJsonWithShortId(section, shortId)).append("\n\n");
            }
        }
        return sb.toString();
    }

    /**
     * 构建单个 section 的 JSON，仅替换 section 级别的 id 为简短标识符
     * 统一使用 content 字段存储内容
     */
    private String buildSectionJsonWithShortId(ResumeDetailVO.ResumeSectionVO section, String shortId) {
        // 使用 Map 构建 JSON，避免手动转义
        Map<String, Object> jsonMap = new LinkedHashMap<>();
        jsonMap.put("id", shortId);
        jsonMap.put("type", section.getType());
        if (section.getTitle() != null) {
            jsonMap.put("title", section.getTitle());
        }
        // content 统一存储数据（JSON 字符串），需要先解析为对象再序列化
        if (section.getContent() != null) {
            Object contentObj = JsonParseHelper.parseToEntity(section.getContent(), new TypeReference<Object>() {});
            jsonMap.put("content", contentObj);
        }
        return JsonParseHelper.toJsonString(jsonMap);
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
