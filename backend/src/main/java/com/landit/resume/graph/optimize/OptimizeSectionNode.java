package com.landit.resume.graph.optimize;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.core.type.TypeReference;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.schema.GraphSchemaRegistry;
import com.landit.common.util.ChatClientHelper;
import com.landit.common.util.JsonParseHelper;
import com.landit.resume.dto.DiagnoseResumeResponse;
import com.landit.resume.dto.OptimizeSectionContext;
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
import java.util.stream.Collectors;

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

        // 从状态获取 ID 映射：shortId -> realId
        @SuppressWarnings("unchecked")
        Map<String, String> shortIdToRealIdMap = (Map<String, String>) state
                .value(STATE_SECTION_ID_MAP)
                .orElse(Map.of());

        // 构建包含优化策略的简历区块数据，传递给 AI
        String resumeSectionsWithSuggestions = buildResumeSectionsWithSuggestions(
                resumeDetail.getSections(),
                suggestionsJson,
                shortIdToRealIdMap
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
     *
     * @param sections            简历区块列表
     * @param suggestionsJson     suggestions JSON 字符串
     * @param shortIdToRealIdMap  短ID到真实ID的映射
     * @return 包含优化策略的简历区块 JSON 字符串
     */
    private String buildResumeSectionsWithSuggestions(
            List<ResumeDetailVO.ResumeSectionVO> sections,
            String suggestionsJson,
            Map<String, String> shortIdToRealIdMap
    ) {
        // 使用强类型解析 suggestions
        GraphSchemaRegistry.SuggestionsResponse response = JsonParseHelper.parseToEntity(
                suggestionsJson,
                new TypeReference<GraphSchemaRegistry.SuggestionsResponse>() {}
        );

        // 获取建议列表
        List<DiagnoseResumeResponse.Suggestion> suggestions = response.getSuggestions();
        if (suggestions == null || suggestions.isEmpty()) {
            suggestions = List.of();
        }

        // 构建反向映射：realId -> shortId
        Map<String, String> realIdToShortIdMap = new HashMap<>();
        for (Map.Entry<String, String> entry : shortIdToRealIdMap.entrySet()) {
            realIdToShortIdMap.put(entry.getValue(), entry.getKey());
        }

        // 按 shortId 分组建议
        Map<String, List<DiagnoseResumeResponse.Suggestion>> suggestionsByShortId = suggestions.stream()
                .filter(s -> s.getSectionId() != null)
                .collect(Collectors.groupingBy(DiagnoseResumeResponse.Suggestion::getSectionId));

        // 构建结果列表
        List<OptimizeSectionContext> result = new ArrayList<>();
        for (ResumeDetailVO.ResumeSectionVO section : sections) {
            String realSectionId = section.getId();
            String shortId = realIdToShortIdMap.get(realSectionId);

            // 获取该区块对应的建议列表
            List<DiagnoseResumeResponse.Suggestion> matchedSuggestions = shortId != null
                    ? suggestionsByShortId.getOrDefault(shortId, List.of())
                    : List.of();

            // 转换为简化策略
            List<OptimizeSectionContext.SimplifiedStrategy> simplifiedStrategies = matchedSuggestions.stream()
                    .map(s -> OptimizeSectionContext.SimplifiedStrategy.builder()
                            .title(s.getTitle())
                            .problem(s.getProblem())
                            .direction(s.getDirection())
                            .example(s.getExample())
                            .build())
                    .collect(Collectors.toList());

            // 构建上下文对象
            OptimizeSectionContext context = OptimizeSectionContext.builder()
                    .sectionId(realSectionId)
                    .type(section.getType())
                    .content(section.getContent())
                    .strategies(simplifiedStrategies)
                    .build();

            result.add(context);
        }

        log.info("构建优化上下文完成，共 {} 个区块，其中 {} 个有优化策略",
                result.size(),
                result.stream().filter(r -> !r.getStrategies().isEmpty()).count());

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
