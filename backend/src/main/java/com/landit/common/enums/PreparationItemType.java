package com.landit.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 面试准备项类型枚举
 *
 * @author Azir
 */
@Getter
@AllArgsConstructor
public enum PreparationItemType implements BaseEnum {

    COMPANY_RESEARCH("company_research", "公司调研"),
    JD_KEYWORDS("jd_keywords", "JD关键词"),
    TECH_PREP("tech_prep", "技术准备"),
    BEHAVIORAL("behavioral", "行为面试"),
    CASE_STUDY("case_study", "案例准备"),
    TODO("todo", "准备事项"),
    MANUAL("manual", "手动记录");

    private final String code;
    private final String description;

    public static PreparationItemType fromCode(String code) {
        return BaseEnum.fromCode(PreparationItemType.class, code);
    }

}
