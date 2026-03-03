package com.landit.resume.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.schema.GraphSchemaRegistry;
import com.landit.common.util.ChatClientHelper;
import com.landit.common.util.JsonParseHelper;
import com.landit.resume.dto.OptimizeSectionResponse;
import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.util.ChangeFieldTranslator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;

import static com.landit.resume.graph.ResumeOptimizeGraphConstants.*;

/**
 * 简历内容优化节点
 * 对整份简历进行 AI 优化，使用 JSON Schema 约束输出格式
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
        log.info("=== 简历内容优化 ===");

        ResumeDetailVO resumeDetail = (ResumeDetailVO) state.value(STATE_RESUME_CONTENT).orElse(null);
        String resumeContentJson = JsonParseHelper.toJsonString(resumeDetail.getSections());
        String targetPosition = state.value(STATE_TARGET_POSITION).map(v -> (String) v).orElse(DEFAULT_TARGET_POSITION);
        String suggestions = state.value(STATE_SUGGESTIONS).map(v -> (String) v).orElse(DEFAULT_EMPTY_ARRAY);

        // beforeSection: 原始简历的 sections 内容
        List<ResumeDetailVO.ResumeSectionVO> beforeSection = resumeDetail.getSections();

        String prompt = buildPrompt(targetPosition, resumeContentJson, suggestions);
        String optimizeResult = callAI(prompt);

        log.info("简历内容优化完成");

        OptimizeSectionResponse response = JsonParseHelper.parseToEntity(optimizeResult, OptimizeSectionResponse.class);

        // 为每个变更添加翻译字段
        if (response.getChanges() != null) {
            for (OptimizeSectionResponse.Change change : response.getChanges()) {
                change.setTypeLabel(ChangeFieldTranslator.translateType(change.getType()));
                change.setFieldLabel(ChangeFieldTranslator.translateField(change.getField()));
            }
        }

        List<OptimizeSectionResponse.Change> changes = response.getChanges() != null ? response.getChanges() : List.of();
        int changeCount = changes.size();

        // afterSection: 根据 changes 计算优化后的简历内容
        List<?> afterSection = applyChangesToResume(resumeDetail.getSections(), changes);

        Map<String, Object> nodeOutput = buildNodeOutput(response, changes, changeCount, beforeSection, afterSection);

        return Map.of(
                STATE_OPTIMIZED_SECTIONS, optimizeResult,
                STATE_MESSAGES, List.of("简历内容优化完成", "共优化 " + changeCount + " 处"),
                STATE_CURRENT_STEP, NODE_OPTIMIZE_SECTION,
                STATE_NODE_OUTPUT, nodeOutput,
                STATE_CHANGES, changes,
                STATE_IMPROVEMENT_SCORE, response.getImprovementScore()
        );
    }

    private String buildPrompt(String targetPosition, String resumeContent, String suggestions) {
        return aiPromptProperties.getGraph().getOptimizeSection()
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

    /**
     * 将变更列表应用到原始简历数据，生成优化后的简历
     *
     * @param sections 原始简历的 sections
     * @param changes  变更列表
     * @return 优化后的简历 sections
     */
    @SuppressWarnings("unchecked")
    private List<?> applyChangesToResume(List<ResumeDetailVO.ResumeSectionVO> sections, List<OptimizeSectionResponse.Change> changes) {
        if (sections == null) {
            return null;
        }

        // 将 sections 转换为 List<Map> 进行深拷贝和修改
        String json = JsonParseHelper.toJsonString(sections);
        List<Map<String, Object>> sectionsList = JsonParseHelper.parseToEntity(json, List.class);

        for (OptimizeSectionResponse.Change change : changes) {
            String field = change.getField();
            String type = change.getType();
            Object value = change.getAfter();

            try {
                applyChangeToSections(sectionsList, field, type, value);
            } catch (Exception e) {
                log.warn("应用变更失败: field={}, type={}, error={}", field, type, e.getMessage());
            }
        }

        return sectionsList;
    }

    /**
     * 将单个变更应用到 sections 列表中
     * 支持路径格式：basicInfo.name, education[0].school, work[0].description
     *
     * @param sections sections 列表
     * @param field    字段路径
     * @param type     变更类型 (added/modified/removed)
     * @param value    新值
     */
    @SuppressWarnings("unchecked")
    private void applyChangeToSections(List<Map<String, Object>> sections, String field, String type, Object value) {
        if (field == null || field.isEmpty()) {
            return;
        }

        String[] parts = field.split("\\.");
        Object current = sections;

        // 遍历路径，找到目标位置
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            PathParseResult parseResult = parsePathPart(part);

            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(parseResult.key);
            } else if (current instanceof List && parseResult.index >= 0) {
                List<?> list = (List<?>) current;
                if (parseResult.index < list.size()) {
                    current = list.get(parseResult.index);
                } else {
                    return;
                }
            } else {
                return;
            }

            if (current == null) {
                return;
            }
        }

        // 处理最后一部分
        String lastPart = parts[parts.length - 1];
        PathParseResult lastParseResult = parsePathPart(lastPart);

        if (current instanceof Map) {
            Map<String, Object> targetMap = (Map<String, Object>) current;

            if ("removed".equals(type)) {
                targetMap.remove(lastParseResult.key);
            } else {
                targetMap.put(lastParseResult.key, value);
            }
        } else if (current instanceof List && lastParseResult.index >= 0) {
            List<Object> list = (List<Object>) current;
            if ("removed".equals(type) && lastParseResult.index < list.size()) {
                list.remove(lastParseResult.index);
            } else if (lastParseResult.index < list.size()) {
                list.set(lastParseResult.index, value);
            }
        }
    }

    /**
     * 解析路径部分，提取 key 和数组索引
     * 例如: "education[0]" -> key="education", index=0
     *       "basicInfo" -> key="basicInfo", index=-1
     */
    private PathParseResult parsePathPart(String part) {
        int bracketIndex = part.indexOf('[');
        if (bracketIndex >= 0 && part.endsWith("]")) {
            String key = part.substring(0, bracketIndex);
            int index = Integer.parseInt(part.substring(bracketIndex + 1, part.length() - 1));
            return new PathParseResult(key, index);
        }
        return new PathParseResult(part, -1);
    }

    /**
     * 路径解析结果
     */
    private record PathParseResult(String key, int index) {
    }
}
