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

    BASIC_INFO("BASIC_INFO", "基本信息", false, ResumeStructuredData.BasicInfo.class),
    EDUCATION("EDUCATION", "教育经历", true, ResumeStructuredData.EducationExperience.class),
    WORK("WORK", "工作经历", true, ResumeStructuredData.WorkExperience.class),
    PROJECT("PROJECT", "项目经历", true, ResumeStructuredData.ProjectExperience.class),
    SKILLS("SKILLS", "专业技能", false, null),
    CERTIFICATE("CERTIFICATE", "证书荣誉", true, ResumeStructuredData.Certificate.class),
    RAW_TEXT("RAW_TEXT", "原始文本", false, null);

    private final String code;
    private final String description;
    // 是否为聚合类型（可能有多条记录）
    private final boolean aggregate;
    // 对应的Schema DTO类
    private final Class<?> schemaClass;

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
