package com.landit.resume.util;

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
            "projects", "项目经验",
            "skills", "专业技能",
            "certificates", "证书荣誉",
            "openSource", "开源贡献"
    );

    // ==================== 基本信息字段翻译 ====================
    private static final Map<String, String> BASIC_INFO_FIELDS = Map.of(
            "name", "姓名",
            "gender", "性别",
            "phone", "电话",
            "email", "邮箱",
            "targetPosition", "求职意向",
            "summary", "个人简介"
    );

    // ==================== 教育经历字段翻译 ====================
    private static final Map<String, String> EDUCATION_FIELDS = Map.of(
            "school", "学校名称",
            "degree", "学历",
            "major", "专业",
            "period", "时间段"
    );

    // ==================== 工作经历字段翻译 ====================
    private static final Map<String, String> WORK_FIELDS = Map.of(
            "company", "公司名称",
            "position", "职位",
            "period", "时间段",
            "description", "工作描述"
    );

    // ==================== 项目经验字段翻译 ====================
    private static final Map<String, String> PROJECT_FIELDS = Map.of(
            "name", "项目名称",
            "role", "项目角色",
            "period", "时间段",
            "description", "项目描述",
            "achievements", "项目成果"
    );

    // ==================== 证书字段翻译 ====================
    private static final Map<String, String> CERTIFICATE_FIELDS = Map.of(
            "name", "证书名称",
            "date", "获得日期"
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
        if (field == null || field.isEmpty()) {
            return "未知字段";
        }

        // 处理数组索引格式：section[0].field
        Matcher arrayMatcher = ARRAY_INDEX_PATTERN.matcher(field);
        if (arrayMatcher.matches()) {
            String section = arrayMatcher.group(1);
            int index = Integer.parseInt(arrayMatcher.group(2));
            String subField = arrayMatcher.group(3);

            String sectionLabel = SECTION_LABELS.getOrDefault(section, section);
            int displayIndex = index + 1; // 转换为1-based索引

            if (subField == null || subField.isEmpty()) {
                // 只有数组索引，没有子字段：skills[0]
                return sectionLabel + "[" + displayIndex + "]";
            }

            // 有子字段：education[0].school
            String fieldLabel = getFieldLabel(section, subField);
            return sectionLabel + "[" + displayIndex + "]-" + fieldLabel;
        }

        // 处理简单路径：section.field 或 section
        String[] parts = field.split("\\.", 2);
        String section = parts[0];

        if (parts.length == 1) {
            // 没有子字段，直接返回区块名称
            return SECTION_LABELS.getOrDefault(section, section);
        }

        // 有子字段
        String subField = parts[1];
        String sectionLabel = SECTION_LABELS.getOrDefault(section, section);
        String fieldLabel = getFieldLabel(section, subField);

        return sectionLabel + "-" + fieldLabel;
    }

    /**
     * 获取子字段的翻译
     */
    private static String getFieldLabel(String section, String subField) {
        // 处理嵌套字段：achievements[0]
        Matcher nestedArrayMatcher = ARRAY_INDEX_PATTERN.matcher(subField);
        if (nestedArrayMatcher.matches()) {
            String nestedField = nestedArrayMatcher.group(1);
            String nestedIndex = nestedArrayMatcher.group(2);
            String baseLabel = getBaseFieldLabel(section, nestedField);
            return baseLabel + "[" + (Integer.parseInt(nestedIndex) + 1) + "]";
        }

        return getBaseFieldLabel(section, subField);
    }

    /**
     * 获取基础字段标签
     */
    private static String getBaseFieldLabel(String section, String fieldName) {
        return switch (section) {
            case "basicInfo" -> BASIC_INFO_FIELDS.getOrDefault(fieldName, fieldName);
            case "education" -> EDUCATION_FIELDS.getOrDefault(fieldName, fieldName);
            case "work" -> WORK_FIELDS.getOrDefault(fieldName, fieldName);
            case "projects" -> PROJECT_FIELDS.getOrDefault(fieldName, fieldName);
            case "certificates" -> CERTIFICATE_FIELDS.getOrDefault(fieldName, fieldName);
            case "openSource" -> OPEN_SOURCE_FIELDS.getOrDefault(fieldName, fieldName);
            default -> fieldName;
        };
    }
}
