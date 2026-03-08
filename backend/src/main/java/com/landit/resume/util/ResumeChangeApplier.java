package com.landit.resume.util;

import com.landit.common.enums.SectionType;
import com.landit.common.util.JsonParseHelper;
import com.landit.resume.dto.OptimizeSectionResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 简历变更应用工具
 * 将 AI 生成的变更列表应用到简历数据
 *
 * @author Azir
 */
@Slf4j
public final class ResumeChangeApplier {

    private ResumeChangeApplier() {
    }

    /**
     * 匹配数组索引的正则：section[0].field 或 section[0]
     */
    private static final Pattern ARRAY_INDEX_PATTERN = Pattern.compile("(\\w+)\\[(\\d+)](?:\\.(.+))?");

    /**
     * 字段名到 SectionType 的映射（基于 SectionType.getSchemaFieldName() 反向映射）
     */
    private static final Map<String, SectionType> FIELD_TO_SECTION_TYPE = Arrays.stream(SectionType.values())
            .filter(type -> type.getSchemaFieldName() != null)
            .collect(Collectors.toMap(SectionType::getSchemaFieldName, type -> type));

    /**
     * 将变更列表应用到原始简历数据，生成优化后的简历
     *
     * @param sections 原始简历的 sections
     * @param changes  变更列表
     * @return 优化后的简历 sections
     */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> applyChanges(List<?> sections, List<OptimizeSectionResponse.Change> changes) {
        if (sections == null) {
            return null;
        }
        // 将 sections 转换为 List<Map> 进行深拷贝和修改
        String json = JsonParseHelper.toJsonString(sections);
        List<Map<String, Object>> sectionsList = JsonParseHelper.parseToEntity(json, List.class);
        if (changes == null || changes.isEmpty()) {
            return sectionsList;
        }
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
     *
     * @param sections sections 列表
     * @param field    字段路径（如 work[0].description, basicInfo.name）
     * @param type     变更类型 (added/modified/removed)
     * @param value    新值
     */
    @SuppressWarnings("unchecked")
    private static void applyChangeToSections(List<Map<String, Object>> sections, String field, String type, Object value) {
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
            applyChangeToBasicInfo(targetSection, pathInfo, type, value);
        } else if (sectionType == SectionType.SKILLS) {
            applyChangeToSkills(targetSection, pathInfo, type, value);
        } else {
            applyChangeToAggregateSection(targetSection, pathInfo, type, value);
        }
    }

    /**
     * 根据 SectionType 定位目标 section
     */
    private static Map<String, Object> findTargetSection(List<Map<String, Object>> sections, SectionType sectionType) {
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
    private static void applyChangeToBasicInfo(Map<String, Object> section,
                                                ChangePathInfo pathInfo, String type, Object value) {
        Map<String, Object> content = (Map<String, Object>) section.get("content");
        if (content == null) {
            content = new LinkedHashMap<>();
            section.put("content", content);
        }
        String property = pathInfo.getPropertyPath();
        if (property == null) {
            return;
        }
        if ("removed".equals(type)) {
            content.remove(property);
        } else {
            content.put(property, value);
        }
    }

    /**
     * 应用变更到 SKILLS 类型（特殊处理）
     * SKILLS 结构：items[0].content = "{...}" (JSON 字符串)，其中 skills = [{name, description, level, category}]
     */
    @SuppressWarnings("unchecked")
    private static void applyChangeToSkills(Map<String, Object> section,
                                             ChangePathInfo pathInfo, String type, Object value) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) section.get("items");
        if (items == null || items.isEmpty()) {
            items = new ArrayList<>();
            section.put("items", items);
            Map<String, Object> newItem = new LinkedHashMap<>();
            newItem.put("content", "{}");
            items.add(newItem);
        }
        Map<String, Object> firstItem = items.get(0);
        Map<String, Object> content = resolveContentToMap(firstItem.get("content"));
        if (content == null) {
            content = new LinkedHashMap<>();
        }
        List<Map<String, Object>> skillsList = (List<Map<String, Object>>) content.get("skills");
        if (skillsList == null) {
            skillsList = new ArrayList<>();
            content.put("skills", skillsList);
        }
        int index = pathInfo.getArrayIndex();
        if ("added".equals(type)) {
            if (value instanceof Map) {
                skillsList.add((Map<String, Object>) value);
            } else if (value instanceof String) {
                Map<String, Object> skillMap = new LinkedHashMap<>();
                skillMap.put("name", value);
                skillMap.put("description", "");
                skillMap.put("level", "");
                skillMap.put("category", "");
                skillsList.add(skillMap);
            }
        } else if ("removed".equals(type) && index >= 0 && index < skillsList.size()) {
            skillsList.remove(index);
        } else if (index >= 0 && index < skillsList.size() && value instanceof Map) {
            skillsList.set(index, (Map<String, Object>) value);
        }
        // 修改后将 content 序列化回 JSON 字符串
        firstItem.put("content", JsonParseHelper.toJsonString(content));
    }

    /**
     * 应用变更到聚合类型（WORK/PROJECT/EDUCATION/CERTIFICATE/OPEN_SOURCE）
     * 聚合类型结构：items = [{ id: "...", content: "{...}" }]  // content 是 JSON 字符串
     */
    @SuppressWarnings("unchecked")
    private static void applyChangeToAggregateSection(Map<String, Object> section,
                                                       ChangePathInfo pathInfo, String type, Object value) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) section.get("items");
        if (items == null) {
            items = new ArrayList<>();
            section.put("items", items);
        }
        int index = pathInfo.getArrayIndex();
        String property = pathInfo.getPropertyPath();
        if ("added".equals(type)) {
            // 新增：创建新的 item，content 存储为 JSON 字符串
            if (value instanceof Map) {
                Map<String, Object> newItem = new LinkedHashMap<>();
                newItem.put("id", UUID.randomUUID().toString());
                newItem.put("content", JsonParseHelper.toJsonString(value));
                items.add(newItem);
            } else if (value instanceof String && property == null) {
                Map<String, Object> newItem = new LinkedHashMap<>();
                newItem.put("id", UUID.randomUUID().toString());
                Map<String, Object> contentMap = new LinkedHashMap<>();
                contentMap.put("name", value);
                newItem.put("content", JsonParseHelper.toJsonString(contentMap));
                items.add(newItem);
            }
        } else if ("removed".equals(type) && index >= 0 && index < items.size()) {
            items.remove(index);
        } else if (index >= 0 && index < items.size()) {
            // 修改：更新指定 item 的 content
            Map<String, Object> item = items.get(index);
            Map<String, Object> content = resolveContentToMap(item.get("content"));
            if (content == null) {
                content = new LinkedHashMap<>();
            }
            if (property != null) {
                applyPropertyChange(content, property, type, value);
                // 修改后将 Map 序列化回 JSON 字符串
                item.put("content", JsonParseHelper.toJsonString(content));
            }
        }
    }

    /**
     * 应用嵌套属性变更（处理 achievements[0] 这类数组属性）
     */
    @SuppressWarnings("unchecked")
    private static void applyPropertyChange(Map<String, Object> content, String property,
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

    /**
     * 将 content 解析为 Map
     * content 可能是 JSON 字符串或已经是 Map 对象
     *
     * @param contentObj content 对象
     * @return 解析后的 Map，解析失败返回 null
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> resolveContentToMap(Object contentObj) {
        if (contentObj == null) {
            return null;
        }
        if (contentObj instanceof Map) {
            return (Map<String, Object>) contentObj;
        }
        if (contentObj instanceof String) {
            return JsonParseHelper.parseToMap((String) contentObj);
        }
        return null;
    }

    /**
     * 解析 AI 生成的变更路径
     *
     * @param field 字段路径
     * @return 解析结果
     */
    private static ChangePathInfo parseChangePath(String field) {
        ChangePathInfo info = new ChangePathInfo();
        info.setOriginalPath(field);
        Matcher matcher = ARRAY_INDEX_PATTERN.matcher(field);
        if (matcher.matches()) {
            // 格式：section[index] 或 section[index].property
            info.setSectionFieldName(matcher.group(1));
            info.setArrayIndex(Integer.parseInt(matcher.group(2)));
            info.setPropertyPath(matcher.group(3));
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
