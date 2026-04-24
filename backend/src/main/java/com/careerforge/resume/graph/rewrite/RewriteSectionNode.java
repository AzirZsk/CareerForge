package com.careerforge.resume.graph.rewrite;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.core.type.TypeReference;
import com.careerforge.common.config.prompt.RewritePromptProperties;
import com.careerforge.common.config.prompt.PromptConfig;
import com.careerforge.common.schema.GraphSchemaRegistry;
import com.careerforge.common.util.ChatClientHelper;
import com.careerforge.common.util.JsonParseHelper;
import com.careerforge.resume.dto.DiagnoseResumeResponse;
import com.careerforge.resume.dto.OptimizeSectionContext;
import com.careerforge.resume.dto.OptimizeSectionResponse;
import com.careerforge.resume.dto.ResumeDetailVO;
import com.careerforge.resume.util.ChangeFieldTranslator;
import com.careerforge.resume.util.ResumeChangeApplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.careerforge.resume.graph.rewrite.RewriteGraphConstants.*;

/**
 * 风格改写节点
 * 根据风格差异策略，改写简历内容，输出与 OptimizeSectionNode 一致的 changes 格式
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class RewriteSectionNode implements NodeAction {

    private final ChatClient chatClient;
    private final RewritePromptProperties promptProperties;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始应用风格改写 ===");
        ResumeDetailVO resumeDetail = (ResumeDetailVO) state.value(STATE_RESUME_CONTENT).orElse(null);
        String strategiesJson = (String) state.value(STATE_OPTIMIZED_SECTIONS).orElse(DEFAULT_EMPTY_JSON);
        String targetPosition = resumeDetail.getTargetPosition() != null
                ? resumeDetail.getTargetPosition()
                : DEFAULT_TARGET_POSITION;
        // 获取原始简历区块（用于对比）
        List<ResumeDetailVO.ResumeSectionVO> beforeSection = resumeDetail.getSections();
        // 获取ID映射
        @SuppressWarnings("unchecked")
        Map<String, String> shortIdToRealIdMap = (Map<String, String>) state
                .value(STATE_SECTION_ID_MAP)
                .orElse(Map.of());
        // 构建包含改写策略的简历区块数据
        String resumeSectionsWithStrategies = buildResumeSectionsWithStrategies(
                resumeDetail.getSections(),
                strategiesJson,
                shortIdToRealIdMap
        );
        // 调用AI进行风格改写
        PromptConfig promptConfig = promptProperties.getRewriteSectionConfig();
        String systemPrompt = promptConfig.getSystemPrompt();
        String userPrompt = ChatClientHelper.renderTemplate(
                promptConfig.getUserPromptTemplate(),
                Map.of("targetPosition", targetPosition, "resumeSections", resumeSectionsWithStrategies)
        );
        OptimizeSectionResponse response = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, OptimizeSectionResponse.class
        );
        log.info("风格改写完成:{}", JsonParseHelper.toJsonString(response));
        // 翻译字段标签
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
        // 计算改写后的简历内容
        List<ResumeDetailVO.ResumeSectionVO> afterSection = ResumeChangeApplier.applyChanges(
                resumeDetail.getSections(),
                changes
        );
        // 构建节点输出
        Map<String, Object> nodeOutput = buildNodeOutput(response, changes, changeCount, beforeSection, afterSection);

        return Map.of(
                STATE_OPTIMIZED_SECTIONS, JsonParseHelper.toJsonString(response),
                STATE_MESSAGES, List.of("风格改写完成", "共改写 " + changeCount + " 处"),
                STATE_CURRENT_STEP, NODE_REWRITE_SECTION,
                STATE_NODE_OUTPUT, nodeOutput,
                STATE_CHANGES, changes,
                STATE_IMPROVEMENT_SCORE, response.getImprovementScore()
        );
    }

    /**
     * 构建包含风格改写策略的简历区块数据（复用 OptimizeSectionContext 格式）
     */
    private String buildResumeSectionsWithStrategies(
            List<ResumeDetailVO.ResumeSectionVO> sections,
            String strategiesJson,
            Map<String, String> shortIdToRealIdMap) {
        // 解析策略
        GraphSchemaRegistry.SuggestionsResponse response = JsonParseHelper.parseToEntity(
                strategiesJson,
                new TypeReference<GraphSchemaRegistry.SuggestionsResponse>() {}
        );
        List<DiagnoseResumeResponse.Suggestion> strategies = response.getSuggestions();
        if (strategies == null || strategies.isEmpty()) {
            strategies = List.of();
        }
        // 反向映射：realId -> shortId
        Map<String, String> realIdToShortIdMap = new HashMap<>();
        for (Map.Entry<String, String> entry : shortIdToRealIdMap.entrySet()) {
            realIdToShortIdMap.put(entry.getValue(), entry.getKey());
        }
        // 按 shortId 分组策略
        Map<String, List<DiagnoseResumeResponse.Suggestion>> strategiesByShortId = strategies.stream()
                .filter(s -> s.getSectionId() != null)
                .collect(Collectors.groupingBy(DiagnoseResumeResponse.Suggestion::getSectionId));
        // 构建结果
        List<OptimizeSectionContext> result = new ArrayList<>();
        for (ResumeDetailVO.ResumeSectionVO section : sections) {
            String realSectionId = section.getId();
            String shortId = realIdToShortIdMap.get(realSectionId);
            List<DiagnoseResumeResponse.Suggestion> matchedStrategies = shortId != null
                    ? strategiesByShortId.getOrDefault(shortId, List.of())
                    : List.of();
            List<OptimizeSectionContext.SimplifiedStrategy> simplifiedStrategies = matchedStrategies.stream()
                    .map(s -> OptimizeSectionContext.SimplifiedStrategy.builder()
                            .title(s.getTitle())
                            .problem(s.getProblem())
                            .direction(s.getDirection())
                            .example(s.getExample())
                            .build())
                    .collect(Collectors.toList());
            OptimizeSectionContext context = OptimizeSectionContext.builder()
                    .sectionId(realSectionId)
                    .type(section.getType())
                    .content(section.getContent())
                    .strategies(simplifiedStrategies)
                    .build();
            result.add(context);
        }
        log.info("构建改写上下文完成，共 {} 个区块，其中 {} 个有改写策略",
                result.size(),
                result.stream().filter(r -> !r.getStrategies().isEmpty()).count());
        return JsonParseHelper.toJsonString(result);
    }

    /**
     * 构建节点输出数据
     */
    private Map<String, Object> buildNodeOutput(OptimizeSectionResponse response,
                                                List<OptimizeSectionResponse.Change> changes, int changeCount,
                                                List<ResumeDetailVO.ResumeSectionVO> beforeSection,
                                                List<ResumeDetailVO.ResumeSectionVO> afterSection) {
        return JsonParseHelper.buildNodeOutput(
                NODE_REWRITE_SECTION,
                80,
                "风格改写完成，共 " + changeCount + " 处变更",
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
