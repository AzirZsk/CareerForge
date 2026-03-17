package com.landit.resume.graph.optimize;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.core.type.TypeReference;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.util.ChatClientHelper;
import com.landit.common.util.JsonParseHelper;
import com.landit.resume.dto.OptimizeSectionResponse;
import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.util.ChangeFieldTranslator;
import com.landit.resume.util.ResumeChangeApplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.landit.resume.graph.optimize.ResumeOptimizeGraphConstants.*;

/**
 * 简历内容优化节点
 * 对整份简历进行 AI 优化，使用 JSON Schema 约束输出格式
 * 使用拆分提示词方式调用以利用前缀缓存优化
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class OptimizeSectionNode implements NodeAction {

    private final ChatClient chatClient;
    private final AIPromptProperties aiPromptProperties;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 简历内容优化 ===");
        ResumeDetailVO resumeDetail = (ResumeDetailVO) state.value(STATE_RESUME_CONTENT).orElse(null);
        String suggestionsJson = (String) state.value(STATE_SUGGESTIONS).orElse(DEFAULT_EMPTY_JSON);
        String targetPosition = resumeDetail.getTargetPosition() != null
                ? resumeDetail.getTargetPosition()
                : DEFAULT_TARGET_POSITION;
        // beforeSection: 原始简历的 sections 内容
        List<ResumeDetailVO.ResumeSectionVO> beforeSection = resumeDetail.getSections();

        // 构建包含优化策略的简历区块数据，传递给 AI
        String resumeSectionsWithSuggestions = buildResumeSectionsWithSuggestions(
                resumeDetail.getSections(),
                suggestionsJson
        );

        // 使用拆分提示词调用（前缀缓存优化）
        AIPromptProperties.PromptConfig promptConfig = aiPromptProperties.getGraph().getOptimizeSectionConfig();
        String systemPrompt = promptConfig.getSystemPrompt();
        String userPrompt = ChatClientHelper.renderTemplate(
                promptConfig.getUserPromptTemplate(),
                Map.of("targetPosition", targetPosition, "resumeSections", resumeSectionsWithSuggestions)
        );
        OptimizeSectionResponse response = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, OptimizeSectionResponse.class
        );
        log.info("简历内容优化完成:{}", JsonParseHelper.toJsonString(response));
        // 为每个变更添加翻译字段
        if (response.getChanges() != null) {
            String sectionsJson = JsonParseHelper.toJsonString(resumeDetail.getSections());
            List<Map<String, Object>> sectionsForTranslate = JsonParseHelper.parseToEntity(sectionsJson, new TypeReference<>() {
            });
            for (OptimizeSectionResponse.Change change : response.getChanges()) {
                change.setTypeLabel(ChangeFieldTranslator.translateType(change.getType()));
                change.setFieldLabel(ChangeFieldTranslator.translateField(change.getField(), sectionsForTranslate));
            }
        }
        List<OptimizeSectionResponse.Change> changes = response.getChanges() != null ? response.getChanges() : List.of();
        int changeCount = changes.size();
        // afterSection: 根据 changes 计算优化后的简历内容
        List<ResumeDetailVO.ResumeSectionVO> afterSection = ResumeChangeApplier.applyChanges(
                resumeDetail.getSections(),
                changes
        );
        Map<String, Object> nodeOutput = buildNodeOutput(response, changes, changeCount, beforeSection, afterSection);
        String optimizeResult = JsonParseHelper.toJsonString(response);
        return Map.of(
                STATE_OPTIMIZED_SECTIONS, optimizeResult,
                STATE_MESSAGES, List.of("简历内容优化完成", "共优化 " + changeCount + " 处"),
                STATE_CURRENT_STEP, NODE_OPTIMIZE_SECTION,
                STATE_NODE_OUTPUT, nodeOutput,
                STATE_CHANGES, changes,
                STATE_IMPROVEMENT_SCORE, response.getImprovementScore()
        );
    }

    /**
     * 构建包含优化策略的简历区块数据
     * 将 GenerateSuggestionsNode 输出的 suggestions 合并到对应的简历区块中
     */
    private String buildResumeSectionsWithSuggestions(
            List<ResumeDetailVO.ResumeSectionVO> sections,
            String suggestionsJson
    ) {
        // 解析 suggestions JSON
        Map<String, Object> suggestionsData = JsonParseHelper.parseToEntity(
                suggestionsJson,
                new TypeReference<>() {}
        );

        // 提取 suggestions 列表
        List<Map<String, Object>> suggestionsList = new ArrayList<>();
        if (suggestionsData.get("suggestions") instanceof List) {
            suggestionsList = (List<Map<String, Object>>) suggestionsData.get("suggestions");
        }

        // 按 sectionId 分组
        Map<String, List<Map<String, Object>>> suggestionsBySectionId = new HashMap<>();
        for (Map<String, Object> suggestion : suggestionsList) {
            String sectionId = (String) suggestion.get("sectionId");
            if (sectionId != null) {
                suggestionsBySectionId.computeIfAbsent(sectionId, k -> new ArrayList<>()).add(suggestion);
            }
        }

        // 构建结果：将 strategies 合并到每个 section
        List<Map<String, Object>> result = new ArrayList<>();
        for (ResumeDetailVO.ResumeSectionVO section : sections) {
            Map<String, Object> sectionWithStrategies = new HashMap<>();
            sectionWithStrategies.put("sectionId", section.getId());
            sectionWithStrategies.put("type", section.getType());
            sectionWithStrategies.put("content", section.getContent());

            // 获取该区块对应的优化策略
            String sectionId = section.getId();
            List<Map<String, Object>> strategies = suggestionsBySectionId.getOrDefault(sectionId, new ArrayList<>());

            // 只提取优化所需的字段：title, problem, direction, example
            List<Map<String, String>> simplifiedStrategies = new ArrayList<>();
            for (Map<String, Object> strategy : strategies) {
                Map<String, String> simplified = new HashMap<>();
                simplified.put("title", (String) strategy.get("title"));
                simplified.put("problem", (String) strategy.get("problem"));
                simplified.put("direction", (String) strategy.get("direction"));
                simplified.put("example", (String) strategy.get("example"));
                simplifiedStrategies.add(simplified);
            }
            sectionWithStrategies.put("strategies", simplifiedStrategies);

            result.add(sectionWithStrategies);
        }

        return JsonParseHelper.toJsonString(result);
    }

    private Map<String, Object> buildNodeOutput(OptimizeSectionResponse response,
                                                List<OptimizeSectionResponse.Change> changes, int changeCount,
                                                List<ResumeDetailVO.ResumeSectionVO> beforeSection,
                                                List<ResumeDetailVO.ResumeSectionVO> afterSection) {
        return JsonParseHelper.buildNodeOutput(
                NODE_OPTIMIZE_SECTION,
                80,
                "内容优化完成，共 " + changeCount + " 处变更",
                Map.of(
                        "changes", changes,
                        "improvementScore", response.getImprovementScore(),
                        "tips", response.getTips(),
                        "changeCount", changeCount,
                        "beforeSection", beforeSection,
                        "afterSection", afterSection
                )
        );
    }
}
