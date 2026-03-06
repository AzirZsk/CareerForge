package com.landit.common.enums;

import com.landit.resume.dto.ResumeStructuredData;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 简历模块类型枚举
 *
 * @author Azir
 */
@Getter
@AllArgsConstructor
public enum SectionType {

    BASIC_INFO("BASIC_INFO", "基本信息", false, ResumeStructuredData.BasicInfo.class, "basicInfo"),
    EDUCATION("EDUCATION", "教育经历", true, ResumeStructuredData.EducationExperience.class, "education"),
    WORK("WORK", "工作经历", true, ResumeStructuredData.WorkExperience.class, "work"),
    PROJECT("PROJECT", "项目经历", true, ResumeStructuredData.ProjectExperience.class, "projects"),
    SKILLS("SKILLS", "专业技能", true, ResumeStructuredData.Skill.class, "skills"),
    CERTIFICATE("CERTIFICATE", "证书荣誉", true, ResumeStructuredData.Certificate.class, "certificates"),
    OPEN_SOURCE("OPEN_SOURCE", "开源贡献", true, ResumeStructuredData.OpenSourceContribution.class, "openSource"),
    RAW_TEXT("RAW_TEXT", "原始文本", false, null, null);

    private final String code;
    private final String description;
    /**
     * 是否为聚合类型（可能有多条记录）
     * - true: 数组类型（如教育经历、工作经历等）
     * - false: 单对象类型（如基本信息）
     */
    private final boolean aggregate;
    /**
     * 对应的Schema DTO类
     * - null 表示使用简单类型（如 SKILLS 使用字符串数组）
     */
    private final Class<?> schemaClass;
    /**
     * 在简历 Schema 中的字段名
     * - null 表示不包含在简历 Schema 中（如 RAW_TEXT）
     */
    private final String schemaFieldName;

    /**
     * 根据code获取枚举
     */
    public static SectionType fromCode(String code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(type -> type.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }

}
