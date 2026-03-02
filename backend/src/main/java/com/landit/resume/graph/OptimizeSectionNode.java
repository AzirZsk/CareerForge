package com.landit.resume.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.schema.GraphSchemaRegistry;
import com.landit.common.util.ChatClientHelper;
import com.landit.common.util.JsonParseHelper;
import com.landit.resume.dto.OptimizeSectionResponse;
import com.landit.resume.util.ChangeFieldTranslator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.landit.resume.graph.ResumeOptimizeGraphConstants.*;

/**
 * 模块内容优化节点
 * 对选定的简历模块进行 AI 优化，使用 JSON Schema 约束输出格式
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class OptimizeSectionNode implements NodeAction {

    private final ChatClient chatClient;
    private final AIPromptProperties aiPromptProperties;
    private final GraphSchemaRegistry graphSchemaRegistry;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 模块内容优化 ===");

        String resumeContent = getStateValue(state, STATE_RESUME_CONTENT, DEFAULT_EMPTY_JSON);
        String targetPosition = getStateValue(state, STATE_TARGET_POSITION, DEFAULT_TARGET_POSITION);
        String suggestions = getStateValue(state, STATE_SUGGESTIONS, DEFAULT_EMPTY_ARRAY);
        String sectionType = getStateValue(state, STATE_SECTION_TO_OPTIMIZE, "all");

        String prompt = buildPrompt(sectionType, targetPosition, resumeContent, suggestions);
        String optimizeResult = callAI(prompt);

        log.info("模块优化完成");

        OptimizeSectionResponse response = JsonParseHelper.parseToEntity(optimizeResult, OptimizeSectionResponse.class);

        // 为每个变更添加翻译字段
        if (response.getChanges() != null) {
            for (OptimizeSectionResponse.Change change : response.getChanges()) {
                change.setTypeLabel(ChangeFieldTranslator.translateType(change.getType()));
                change.setFieldLabel(ChangeFieldTranslator.translateField(change.getField()));
            }
        }

        List<?> changes = response.getChanges() != null ? response.getChanges() : List.of();
        int changeCount = changes.size();

        Map<String, Object> nodeOutput = buildNodeOutput(response, changes, changeCount, resumeContent);

        return Map.of(
                STATE_OPTIMIZED_SECTIONS, optimizeResult,
                STATE_MESSAGES, List.of("模块内容优化完成", "共优化 " + changeCount + " 处"),
                STATE_CURRENT_STEP, NODE_OPTIMIZE_SECTION,
                STATE_NODE_OUTPUT, nodeOutput,
                STATE_CHANGES, changes,
                STATE_IMPROVEMENT_SCORE, response.getImprovementScore()
        );
    }

    private String getStateValue(OverAllState state, String key, String defaultValue) {
        return state.value(key).map(v -> (String) v).orElse(defaultValue);
    }

    private String buildPrompt(String sectionType, String targetPosition, String resumeContent, String suggestions) {
        return aiPromptProperties.getGraph().getOptimizeSection()
                .replace("{sectionType}", sectionType)
                .replace("{targetPosition}", targetPosition)
                .replace("{resumeContent}", resumeContent)
                .replace("{suggestions}", suggestions);
    }

    private String callAI(String prompt) {
        return ChatClientHelper.callStreamAndCollectWithSchema(
                chatClient,
                prompt,
                graphSchemaRegistry.buildOptimizeSectionSchema(),
                "OptimizeSectionResponse"
        );
    }

    private Map<String, Object> buildNodeOutput(OptimizeSectionResponse response, List<?> changes, int changeCount, String resumeContent) {
        Map<String, Object> beforeResume = JsonParseHelper.parseToMap(resumeContent);

        return JsonParseHelper.buildNodeOutput(
                NODE_OPTIMIZE_SECTION,
                80,
                "内容优化完成，共 " + changeCount + " 处变更",
                Map.of(
                        "changes", changes,
                        "improvementScore", response.getImprovementScore(),
                        "tips", response.getTips(),
                        "changeCount", changeCount,
                        "beforeResume", beforeResume,
                        "optimizedContent", response.getOptimizedContent()
                )
        );
    }
}
