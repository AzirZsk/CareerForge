package com.landit.resume.graph;

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

import java.util.List;
import java.util.Map;

import static com.landit.resume.graph.ResumeOptimizeGraphConstants.*;

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
        String resumeContentJson = JsonParseHelper.toJsonString(resumeDetail.getSections());
        String targetPosition = resumeDetail.getTargetPosition() != null
                ? resumeDetail.getTargetPosition()
                : DEFAULT_TARGET_POSITION;
        String suggestions = state.value(STATE_SUGGESTIONS).map(v -> (String) v).orElse(DEFAULT_EMPTY_ARRAY);
        // beforeSection: 原始简历的 sections 内容
        List<ResumeDetailVO.ResumeSectionVO> beforeSection = resumeDetail.getSections();
        // 使用拆分提示词调用（前缀缓存优化）
        AIPromptProperties.PromptConfig promptConfig = aiPromptProperties.getGraph().getOptimizeSectionConfig();
        String systemPrompt = promptConfig.getSystemPrompt();
        String userPrompt = ChatClientHelper.renderTemplate(
                promptConfig.getUserPromptTemplate(),
                Map.of("targetPosition", targetPosition, "resumeContent", resumeContentJson, "suggestions", suggestions)
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
        List<?> afterSection = ResumeChangeApplier.applyChanges(resumeDetail.getSections(), changes);
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

    private Map<String, Object> buildNodeOutput(OptimizeSectionResponse response,
                                                List<OptimizeSectionResponse.Change> changes, int changeCount,
                                                List<ResumeDetailVO.ResumeSectionVO> beforeSection, List<?> afterSection) {
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
