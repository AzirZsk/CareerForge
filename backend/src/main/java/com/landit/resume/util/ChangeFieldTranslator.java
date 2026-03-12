package com.landit.resume.util;

import com.landit.common.util.JsonParseHelper;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 变更字段翻译工具
 * 将字段路径和操作类型翻译为中文
 *
 * @author Azir
 */
public class ChangeFieldTranslator {

    private ChangeFieldTranslator() {
    }

    // ==================== 操作类型翻译 ====================
    private static final Map<String, String> TYPE_LABELS = Map.of(
            "added", "新增",
            "modified", "修改",
            "removed", "删除"
    );

    // ==================== 区块名称翻译 ====================
    private static final Map<String, String> SECTION_LABELS = Map.of(
            "basicInfo", "基本信息",
            "education", "教育经历",
            "work", "工作经历",
            "project", "项目经历",
            "projects", "项目经历",
            "skills", "专业技能",
            "certificates", "证书荣誉",
            "openSource", "开源贡献",
            "customSections", "自定义区块"
    );

    // ==================== 基本信息字段翻译 ====================
    private static final Map<String, String> BASIC_INFO_FIELDS = Map.ofEntries(
            Map.entry("name", "姓名"),
            Map.entry("gender", "性别"),
            Map.entry("birthday", "出生日期"),
            Map.entry("age", "年龄"),
            Map.entry("phone", "电话"),
            Map.entry("email", "邮箱"),
            Map.entry("targetPosition", "求职意向"),
            Map.entry("summary", "个人简介"),
            Map.entry("location", "所在地"),
            Map.entry("linkedin", "LinkedIn"),
            Map.entry("github", "GitHub"),
            Map.entry("website", "个人网站")
    );

    // ==================== 教育经历字段翻译 ====================
    private static final Map<String, String> EDUCATION_FIELDS = Map.of(
            "school", "学校名称",
            "degree", "学历",
            "major", "专业",
            "period", "时间段",
            "gpa", "绩点",
            "courses", "主修课程",
            "honors", "校内荣誉"
    );

    // ==================== 工作经历字段翻译 ====================
    private static final Map<String, String> WORK_FIELDS = Map.of(
            "company", "公司名称",
            "position", "职位",
            "period", "时间段",
            "description", "工作描述",
            "location", "工作地点",
            "achievements", "工作成果",
            "technologies", "技术栈"
    );

    // ==================== 项目经验字段翻译 ====================
    private static final Map<String, String> PROJECT_FIELDS = Map.of(
            "name", "项目名称",
            "role", "项目角色",
            "period", "时间段",
            "description", "项目描述",
            "achievements", "项目成果",
            "technologies", "技术栈",
            "url", "项目链接"
    );

    // ==================== 证书字段翻译 ====================
    private static final Map<String, String> CERTIFICATE_FIELDS = Map.of(
            "name", "证书名称",
            "date", "获得日期",
            "issuer", "颁发机构",
            "credentialId", "证书编号",
            "url", "证书链接"
    );

    // ==================== 开源贡献字段翻译 ====================
    private static final Map<String, String> OPEN_SOURCE_FIELDS = Map.of(
            "projectName", "项目名称",
            "url", "项目地址",
            "role", "贡献角色",
            "period", "时间段",
            "description", "贡献描述",
            "achievements", "贡献成果"
    );

    // ==================== 自定义区块字段翻译 ====================
    private static final Map<String, String> CUSTOM_SECTIONS_FIELDS = Map.of(
            "title", "区块标题",
            "items", "内容项列表",
            "name", "名称",
            "role", "角色",
            "period", "时间段",
            "description", "描述",
            "highlights", "亮点"
    );

    // ==================== 技能字段翻译 ====================
    private static final Map<String, String> SKILLS_FIELDS = Map.of(
            "name", "技能名称",
            "description", "技能描述",
            "level", "熟练度",
            "category", "分类"
    );

    // 匹配数组索引的正则：section[0].field 或 section[0]
    private static final Pattern ARRAY_INDEX_PATTERN = Pattern.compile("(\\w+)\\[(\\d+)](?:\\.(.+))?");

    /**
     * 翻译操作类型
     *
     * @param type 操作类型 (added/modified/removed)
     * @return 中文翻译
     */
    public static String translateType(String type) {
        if (type == null) {
            return "未知";
        }
        return TYPE_LABELS.getOrDefault(type.toLowerCase(), type);
    }

    /**
     * 翻译字段路径
     *
     * @param field 字段路径，如 "basicInfo.name" 或 "education[0].school"
     * @return 中文翻译，如 "基本信息-姓名" 或 "教育经历[1]-学校名称"
     */
    public static String translateField(String field) {
        return translateField(field, null);
    }

    /**
     * 翻译字段路径（支持动态获取 customSections 的 title）
     *
     * @param field    字段路径
     * @param sections 简历区块数据，用于获取 customSections 的 title
     * @return 中文翻译
     */
    @SuppressWarnings("unchecked")
    public static String translateField(String field, List<Map<String, Object>> sections) {
        if (field == null || field.isEmpty()) {
            return "未知字段";
        }

        // 处理数组索引格式：section[0].field
        Matcher arrayMatcher = ARRAY_INDEX_PATTERN.matcher(field);
        if (arrayMatcher.matches()) {
            String section = arrayMatcher.group(1);
            int index = Integer.parseInt(arrayMatcher.group(2));
            String subField = arrayMatcher.group(3);

            // 对于 customSections，动态获取 title
            String sectionLabel = getSectionLabel(section, index, sections);
            int displayIndex = index + 1;

            if (subField == null || subField.isEmpty()) {
                return sectionLabel + "[" + displayIndex + "]";
            }

            String fieldLabel = getFieldLabel(section, subField);
            return sectionLabel + "[" + displayIndex + "]-" + fieldLabel;
        }

        // 处理简单路径：section.field 或 section
        String[] parts = field.split("\\.", 2);
        String section = parts[0];

        if (parts.length == 1) {
            return SECTION_LABELS.getOrDefault(section, section);
        }

        String subField = parts[1];
        String sectionLabel = SECTION_LABELS.getOrDefault(section, section);
        String fieldLabel = getFieldLabel(section, subField);

        return sectionLabel + "-" + fieldLabel;
    }

    /**
     * 获取区块名称标签（对于 customSections 直接从 section.title 获取）
     */
    @SuppressWarnings("unchecked")
    private static String getSectionLabel(String section, int index, List<Map<String, Object>> sections) {
        // 对于 customSections，直接从 section.title 获取（每个 CUSTOM 区块独立一条记录）
        if ("customSections".equals(section) && sections != null) {
            // 查找所有 CUSTOM 类型的区块
            List<Map<String, Object>> customSections = sections.stream()
                .filter(s -> "CUSTOM".equals(s.get("type")))
                .toList();
            if (customSections != null && index < customSections.size()) {
                Map<String, Object> customSection = customSections.get(index);
                // 直接从 section.title 获取
                String title = (String) customSection.get("title");
                if (title != null && !title.isEmpty()) {
                    return title;
                }
            }
        }
        return SECTION_LABELS.getOrDefault(section, section);
    }

    /**
     * 将 content 解析为 Map
     * content 可能是 JSON 字符串或已经是 Map 对象
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> resolveContentToMap(Object contentObj) {
        if (contentObj == null) {
            return null;
        }
        // 如果已经是 Map，直接返回
        if (contentObj instanceof Map) {
            return (Map<String, Object>) contentObj;
        }
        // 如果是字符串，解析为 Map
        if (contentObj instanceof String) {
            return JsonParseHelper.parseToMap((String) contentObj);
        }
        return null;
    }

    /**
     * 根据类型查找区块
     */
    private static Map<String, Object> findSectionByType(List<Map<String, Object>> sections, String type) {
        for (Map<String, Object> section : sections) {
            if (type.equals(section.get("type"))) {
                return section;
            }
        }
        return null;
    }

    /**
     * 获取子字段的翻译
     * 支持多层嵌套路径，如 items[0].description
     */
    private static String getFieldLabel(String section, String subField) {
        StringBuilder result = new StringBuilder();
        parseNestedField(section, subField, result);
        return result.toString();
    }

    /**
     * 递归解析嵌套字段路径
     */
    private static void parseNestedField(String section, String subField, StringBuilder result) {
        Matcher nestedArrayMatcher = ARRAY_INDEX_PATTERN.matcher(subField);
        if (nestedArrayMatcher.matches()) {
            String fieldName = nestedArrayMatcher.group(1);
            String indexStr = nestedArrayMatcher.group(2);
            String remaining = nestedArrayMatcher.group(3);

            String baseLabel = getBaseFieldLabel(section, fieldName);
            result.append(baseLabel).append("[").append(Integer.parseInt(indexStr) + 1).append("]");

            if (remaining != null && !remaining.isEmpty()) {
                result.append("-");
                parseNestedField(section, remaining, result);
            }
        } else {
            result.append(getBaseFieldLabel(section, subField));
        }
    }

    /**
     * 获取基础字段标签
     */
    private static String getBaseFieldLabel(String section, String fieldName) {
        return switch (section) {
            case "basicInfo" -> BASIC_INFO_FIELDS.getOrDefault(fieldName, fieldName);
            case "education" -> EDUCATION_FIELDS.getOrDefault(fieldName, fieldName);
            case "work" -> WORK_FIELDS.getOrDefault(fieldName, fieldName);
            case "project", "projects" -> PROJECT_FIELDS.getOrDefault(fieldName, fieldName);
            case "skills" -> SKILLS_FIELDS.getOrDefault(fieldName, fieldName);
            case "certificates" -> CERTIFICATE_FIELDS.getOrDefault(fieldName, fieldName);
            case "openSource" -> OPEN_SOURCE_FIELDS.getOrDefault(fieldName, fieldName);
            case "customSections" -> CUSTOM_SECTIONS_FIELDS.getOrDefault(fieldName, fieldName);
            default -> fieldName;
        };
    }
}
