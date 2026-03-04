package com.landit.resume.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.enums.SectionType;
import com.landit.common.util.ChatClientHelper;
import com.landit.common.util.JsonParseHelper;
import com.landit.resume.dto.OptimizeSectionResponse;
import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.util.ChangeFieldTranslator;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
        String targetPosition = state.value(STATE_TARGET_POSITION).map(v -> (String) v).orElse(DEFAULT_TARGET_POSITION);
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

        log.info("简历内容优化完成");

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
            Object value = change.getAfterValue();

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
     * 根据区块类型定位目标 section，然后应用变更
     *
     * @param sections sections 列表
     * @param field    字段路径（如 work[0].description, basicInfo.name）
     * @param type     变更类型 (added/modified/removed)
     * @param value    新值
     */
    @SuppressWarnings("unchecked")
    private void applyChangeToSections(List<Map<String, Object>> sections, String field, String type, Object value) {
        // 1. 解析路径
        ChangePathInfo pathInfo = parseChangePath(field);
        if (pathInfo.getSectionType() == null) {
            log.warn("无法识别的区块类型: {}", pathInfo.getSectionFieldName());
            return;
        }

        // 2. 定位目标 section
        Map<String, Object> targetSection = findTargetSection(sections, pathInfo.getSectionType());
        if (targetSection == null) {
            log.warn("未找到目标区块: {}", pathInfo.getSectionType());
            return;
        }

        // 3. 根据区块类型应用变更
        SectionType sectionType = pathInfo.getSectionType();

        if (sectionType == SectionType.BASIC_INFO) {
            // 单条类型：直接操作 content 对象
            applyChangeToBasicInfo(targetSection, pathInfo, type, value);
        } else if (sectionType == SectionType.SKILLS) {
            // SKILLS 特殊处理：items[0].content.skills 是数组
            applyChangeToSkills(targetSection, pathInfo, type, value);
        } else {
            // 聚合类型：操作 items[x].content
            applyChangeToAggregateSection(targetSection, pathInfo, type, value);
        }
    }

    /**
     * 根据 SectionType 定位目标 section
     */
    private Map<String, Object> findTargetSection(List<Map<String, Object>> sections, SectionType sectionType) {
        for (Map<String, Object> section : sections) {
            String type = (String) section.get("type");
            if (sectionType.getCode().equals(type)) {
                return section;
            }
        }
        return null;
    }

    /**
     * 应用变更到 BASIC_INFO 类型
     * BASIC_INFO 结构：content = { name: "...", email: "..." }
     */
    @SuppressWarnings("unchecked")
    private void applyChangeToBasicInfo(Map<String, Object> section,
                                         ChangePathInfo pathInfo, String type, Object value) {
        Map<String, Object> content = (Map<String, Object>) section.get("content");
        if (content == null) {
            content = new LinkedHashMap<>();
            section.put("content", content);
        }

        String property = pathInfo.getPropertyPath();
        if (property == null) return;

        if ("removed".equals(type)) {
            content.remove(property);
        } else {
            content.put(property, value);
        }
    }

    /**
     * 应用变更到 SKILLS 类型（特殊处理）
     * SKILLS 结构：items[0].content.skills = ["Java", "Python"]
     */
    @SuppressWarnings("unchecked")
    private void applyChangeToSkills(Map<String, Object> section,
                                      ChangePathInfo pathInfo, String type, Object value) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) section.get("items");
        if (items == null || items.isEmpty()) {
            items = new ArrayList<>();
            section.put("items", items);
            Map<String, Object> newItem = new LinkedHashMap<>();
            newItem.put("content", new LinkedHashMap<String, Object>());
            items.add(newItem);
        }

        Map<String, Object> firstItem = items.get(0);
        Map<String, Object> content = (Map<String, Object>) firstItem.get("content");
        if (content == null) {
            content = new LinkedHashMap<>();
            firstItem.put("content", content);
        }

        List<String> skillsList = (List<String>) content.get("skills");
        if (skillsList == null) {
            skillsList = new ArrayList<>();
            content.put("skills", skillsList);
        }

        int index = pathInfo.getArrayIndex();

        if ("added".equals(type)) {
            skillsList.add((String) value);
        } else if ("removed".equals(type) && index >= 0 && index < skillsList.size()) {
            skillsList.remove(index);
        } else if (index >= 0 && index < skillsList.size()) {
            skillsList.set(index, (String) value);
        }
    }

    /**
     * 应用变更到聚合类型（WORK/PROJECT/EDUCATION/CERTIFICATE/OPEN_SOURCE）
     * 聚合类型结构：items = [{ id: "...", content: {...} }]
     */
    @SuppressWarnings("unchecked")
    private void applyChangeToAggregateSection(Map<String, Object> section,
                                                ChangePathInfo pathInfo, String type, Object value) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) section.get("items");
        if (items == null) {
            items = new ArrayList<>();
            section.put("items", items);
        }

        int index = pathInfo.getArrayIndex();
        String property = pathInfo.getPropertyPath();

        if ("added".equals(type)) {
            // 新增：创建新的 item
            if (value instanceof Map) {
                Map<String, Object> newItem = new LinkedHashMap<>();
                newItem.put("id", UUID.randomUUID().toString());
                newItem.put("content", value);
                items.add(newItem);
            } else if (value instanceof String && property == null) {
                // 简单字符串值（如证书名称），包装为对象
                Map<String, Object> newItem = new LinkedHashMap<>();
                newItem.put("id", UUID.randomUUID().toString());
                Map<String, Object> content = new LinkedHashMap<>();
                content.put("name", value);
                newItem.put("content", content);
                items.add(newItem);
            }
        } else if ("removed".equals(type) && index >= 0 && index < items.size()) {
            items.remove(index);
        } else if (index >= 0 && index < items.size()) {
            // 修改：更新指定 item 的 content
            Map<String, Object> item = items.get(index);
            Map<String, Object> content = (Map<String, Object>) item.get("content");
            if (content == null) {
                content = new LinkedHashMap<>();
                item.put("content", content);
            }

            if (property != null) {
                // 处理嵌套属性（如 achievements[0]）
                applyPropertyChange(content, property, type, value);
            }
        }
    }

    /**
     * 应用嵌套属性变更（处理 achievements[0] 这类数组属性）
     */
    @SuppressWarnings("unchecked")
    private void applyPropertyChange(Map<String, Object> content, String property,
                                      String type, Object value) {
        Matcher matcher = ARRAY_INDEX_PATTERN.matcher(property);
        if (matcher.matches()) {
            String fieldName = matcher.group(1);
            String indexStr = matcher.group(2);
            String subProperty = matcher.group(3);

            if (indexStr != null) {
                // 数组属性操作
                int index = Integer.parseInt(indexStr);
                List<Object> array = (List<Object>) content.get(fieldName);
                if (array == null) {
                    array = new ArrayList<>();
                    content.put(fieldName, array);
                }

                if (subProperty != null) {
                    // 嵌套属性，继续递归处理
                    if (index < array.size() && array.get(index) instanceof Map) {
                        applyPropertyChange((Map<String, Object>) array.get(index), subProperty, type, value);
                    }
                } else {
                    // 直接数组元素操作
                    if ("removed".equals(type) && index < array.size()) {
                        array.remove(index);
                    } else if ("added".equals(type)) {
                        array.add(value);
                    } else if (index < array.size()) {
                        array.set(index, value);
                    } else if (index == array.size()) {
                        array.add(value);
                    }
                }
            } else {
                // 普通属性
                if ("removed".equals(type)) {
                    content.remove(fieldName);
                } else {
                    content.put(fieldName, value);
                }
            }
        } else {
            // 普通属性操作
            if ("removed".equals(type)) {
                content.remove(property);
            } else {
                content.put(property, value);
            }
        }
    }

    // ==================== 路径解析相关 ====================

    /**
     * 字段名到 SectionType 的映射（基于 SectionType.getSchemaFieldName() 反向映射）
     */
    private static final Map<String, SectionType> FIELD_TO_SECTION_TYPE = Arrays.stream(SectionType.values())
            .filter(type -> type.getSchemaFieldName() != null)
            .collect(Collectors.toMap(SectionType::getSchemaFieldName, type -> type));

    /**
     * 匹配数组索引的正则：section[0].field 或 section[0]
     * 复用 ChangeFieldTranslator 的正则表达式模式
     */
    private static final Pattern ARRAY_INDEX_PATTERN = Pattern.compile("(\\w+)\\[(\\d+)](?:\\.(.+))?");

    /**
     * 解析 AI 生成的变更路径
     * <p>
     * 输入示例：
     * - "basicInfo.name" -> sectionFieldName="basicInfo", arrayIndex=-1, propertyPath="name"
     * - "work[0].description" -> sectionFieldName="work", arrayIndex=0, propertyPath="description"
     * - "certificates[0]" -> sectionFieldName="certificates", arrayIndex=0, propertyPath=null
     * - "projects[0].achievements[0]" -> sectionFieldName="projects", arrayIndex=0, propertyPath="achievements[0]"
     */
    private ChangePathInfo parseChangePath(String field) {
        ChangePathInfo info = new ChangePathInfo();
        info.setOriginalPath(field);

        Matcher matcher = ARRAY_INDEX_PATTERN.matcher(field);
        if (matcher.matches()) {
            // 格式：section[index] 或 section[index].property
            info.setSectionFieldName(matcher.group(1));
            info.setArrayIndex(Integer.parseInt(matcher.group(2)));
            info.setPropertyPath(matcher.group(3)); // 可能为 null
        } else {
            // 格式：section.property（无数组索引）
            String[] parts = field.split("\\.", 2);
            info.setSectionFieldName(parts[0]);
            if (parts.length > 1) {
                info.setPropertyPath(parts[1]);
            }
        }

        // 映射到 SectionType
        info.setSectionType(FIELD_TO_SECTION_TYPE.get(info.getSectionFieldName()));
        return info;
    }

    /**
     * 路径解析结果
     */
    @Data
    private static class ChangePathInfo {
        /** 原始字段名（如 work, projects, skills） */
        private String sectionFieldName;
        /** 对应的 SectionType */
        private SectionType sectionType;
        /** 数组索引（-1 表示非数组） */
        private int arrayIndex = -1;
        /** 属性路径（数组索引后的剩余路径） */
        private String propertyPath;
        /** 完整原始路径 */
        private String originalPath;
    }
}
