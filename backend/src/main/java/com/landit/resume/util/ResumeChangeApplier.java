package com.landit.resume.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.landit.common.enums.SectionType;
import com.landit.common.util.JsonParseHelper;
import com.landit.resume.dto.OptimizeSectionResponse;
import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.dto.ResumeStructuredData;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 简历变更应用工具
 * 将 AI 生成的变更列表应用到简历数据
 * <p>
 * 设计说明：
 * - 对外提供强类型接口（ResumeSectionVO）
 * - 内部使用 Map 处理动态属性修改（因为 AI 生成的 field 路径是动态的）
 * - 提供 contentToTypedObject 方法将 Map 转换为强类型对象
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
    public static List<ResumeDetailVO.ResumeSectionVO> applyChanges(
            List<ResumeDetailVO.ResumeSectionVO> sections,
            List<OptimizeSectionResponse.Change> changes) {
        if (sections == null) {
            return null;
        }
        // 使用 TypeReference 进行强类型深拷贝
        String json = JsonParseHelper.toJsonString(sections);
        List<ResumeDetailVO.ResumeSectionVO> sectionsCopy = JsonParseHelper.parseToEntity(json, new TypeReference<>() {}
        );
        if (changes == null || changes.isEmpty()) {
            return sectionsCopy;
        }
        for (OptimizeSectionResponse.Change change : changes) {
            String field = change.getField();
            String type = change.getType();
            Object value = change.getAfterValue();
            try {
                applyChangeToSections(sectionsCopy, field, type, value);
            } catch (Exception e) {
                log.warn("应用变更失败: field={}, type={}, error={}", field, type, e.getMessage());
            }
        }
        return sectionsCopy;
    }

    /**
     * 将 Map 格式的 content 转换为强类型对象
     * 用于外部调用方获取类型安全的 content 数据
     *
     * @param contentMap  content 的 Map 表示
     * @param sectionType 区块类型
     * @param <T>         目标类型
     * @return 强类型对象，转换失败返回 null
     */
    @SuppressWarnings("unchecked")
    public static <T> T contentToTypedObject(Map<String, Object> contentMap, SectionType sectionType) {
        if (contentMap == null || sectionType == null) {
            return null;
        }
        Class<?> schemaClass = sectionType.getSchemaClass();
        if (schemaClass == null) {
            return null;
        }
        try {
            String json = JsonParseHelper.toJsonString(contentMap);
            return (T) JsonParseHelper.parseToEntity(json, schemaClass);
        } catch (Exception e) {
            log.warn("转换强类型失败: sectionType={}, error={}", sectionType, e.getMessage());
            return null;
        }
    }

    /**
     * 将聚合类型的 item content 转换为强类型对象列表
     *
     * @param section     简历区块
     * @param sectionType 区块类型
     * @param <T>         目标类型
     * @return 强类型对象列表
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> itemsToTypedList(ResumeDetailVO.ResumeSectionVO section, SectionType sectionType) {
        if (section == null || section.getItems() == null || sectionType == null) {
            return List.of();
        }
        Class<?> schemaClass = sectionType.getSchemaClass();
        if (schemaClass == null) {
            return List.of();
        }
        List<T> result = new ArrayList<>();
        for (ResumeDetailVO.ResumeSectionItemVO item : section.getItems()) {
            Map<String, Object> contentMap = resolveContentToMap(item.getContent());
            if (contentMap != null) {
                T typedObj = contentToTypedObject(contentMap, sectionType);
                if (typedObj != null) {
                    result.add(typedObj);
                }
            }
        }
        return result;
    }

    /**
     * 获取 BASIC_INFO 的强类型对象
     *
     * @param section 简历区块
     * @return BasicInfo 对象，转换失败返回 null
     */
    public static ResumeStructuredData.BasicInfo getBasicInfo(ResumeDetailVO.ResumeSectionVO section) {
        if (section == null || section.getContent() == null) {
            return null;
        }
        Map<String, Object> contentMap = resolveContentToMap(section.getContent());
        if (contentMap == null) {
            return null;
        }
        return contentToTypedObject(contentMap, SectionType.BASIC_INFO);
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
    private static void applyChangeToSections(List<ResumeDetailVO.ResumeSectionVO> sections,
                                               String field, String type, Object value) {
        // 1. 解析路径
        ChangePathInfo pathInfo = parseChangePath(field);
        if (pathInfo.getSectionType() == null) {
            log.warn("无法识别的区块类型: {}", pathInfo.getSectionFieldName());
            return;
        }
        // 2. 定位目标 section
        ResumeDetailVO.ResumeSectionVO targetSection = findTargetSection(sections, pathInfo.getSectionType());
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
    private static ResumeDetailVO.ResumeSectionVO findTargetSection(
            List<ResumeDetailVO.ResumeSectionVO> sections, SectionType sectionType) {
        for (ResumeDetailVO.ResumeSectionVO section : sections) {
            if (sectionType.getCode().equals(section.getType())) {
                return section;
            }
        }
        return null;
    }

    /**
     * 应用变更到 BASIC_INFO 类型
     * BASIC_INFO 结构：content = "{...}" (JSON 字符串格式)
     */
    private static void applyChangeToBasicInfo(ResumeDetailVO.ResumeSectionVO section,
                                                ChangePathInfo pathInfo, String type, Object value) {
        Map<String, Object> content = resolveContentToMap(section.getContent());
        if (content == null) {
            content = new LinkedHashMap<>();
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
        // 修改后将 content 序列化回 JSON 字符串
        section.setContent(JsonParseHelper.toJsonString(content));
    }

    /**
     * 应用变更到 SKILLS 类型（特殊处理）
     * SKILLS 结构：items[0].content = "{...}" (JSON 字符串)，其中 skills = [{name, description, level, category}]
     */
    @SuppressWarnings("unchecked")
    private static void applyChangeToSkills(ResumeDetailVO.ResumeSectionVO section,
                                             ChangePathInfo pathInfo, String type, Object value) {
        List<ResumeDetailVO.ResumeSectionItemVO> items = section.getItems();
        if (items == null || items.isEmpty()) {
            items = new ArrayList<>();
            section.setItems(items);
            ResumeDetailVO.ResumeSectionItemVO newItem = ResumeDetailVO.ResumeSectionItemVO.builder()
                    .content("{}")
                    .build();
            items.add(newItem);
        }
        ResumeDetailVO.ResumeSectionItemVO firstItem = items.get(0);
        Map<String, Object> content = resolveContentToMap(firstItem.getContent());
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
        firstItem.setContent(JsonParseHelper.toJsonString(content));
    }

    /**
     * 应用变更到聚合类型（WORK/PROJECT/EDUCATION/CERTIFICATE/OPEN_SOURCE）
     * 聚合类型结构：items = [{ id: "...", content: "{...}" }]  // content 是 JSON 字符串
     */
    @SuppressWarnings("unchecked")
    private static void applyChangeToAggregateSection(ResumeDetailVO.ResumeSectionVO section,
                                                       ChangePathInfo pathInfo, String type, Object value) {
        List<ResumeDetailVO.ResumeSectionItemVO> items = section.getItems();
        if (items == null) {
            items = new ArrayList<>();
            section.setItems(items);
        }
        int index = pathInfo.getArrayIndex();
        String property = pathInfo.getPropertyPath();
        if ("added".equals(type)) {
            // 新增：创建新的 item，content 存储为 JSON 字符串
            if (value instanceof Map) {
                ResumeDetailVO.ResumeSectionItemVO newItem = ResumeDetailVO.ResumeSectionItemVO.builder()
                        .id(UUID.randomUUID().toString())
                        .content(JsonParseHelper.toJsonString(value))
                        .build();
                items.add(newItem);
            } else if (value instanceof String && property == null) {
                Map<String, Object> contentMap = new LinkedHashMap<>();
                contentMap.put("name", value);
                ResumeDetailVO.ResumeSectionItemVO newItem = ResumeDetailVO.ResumeSectionItemVO.builder()
                        .id(UUID.randomUUID().toString())
                        .content(JsonParseHelper.toJsonString(contentMap))
                        .build();
                items.add(newItem);
            }
        } else if ("removed".equals(type) && index >= 0 && index < items.size()) {
            items.remove(index);
        } else if (index >= 0 && index < items.size()) {
            // 修改：更新指定 item 的 content
            ResumeDetailVO.ResumeSectionItemVO item = items.get(index);
            Map<String, Object> content = resolveContentToMap(item.getContent());
            if (content == null) {
                content = new LinkedHashMap<>();
            }
            if (property != null) {
                applyPropertyChange(content, property, type, value);
                // 修改后将 Map 序列化回 JSON 字符串
                item.setContent(JsonParseHelper.toJsonString(content));
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
