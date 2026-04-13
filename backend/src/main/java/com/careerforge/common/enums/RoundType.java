package com.careerforge.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 面试轮次类型枚举
 *
 * @author Azir
 */
@Getter
@AllArgsConstructor
public enum RoundType implements BaseEnum {

    TECHNICAL_1("technical_1", "技术一面"),
    TECHNICAL_2("technical_2", "技术二面"),
    HR("hr", "HR面"),
    DIRECTOR("director", "总监面"),
    CTO("cto", "CTO/VP面"),
    FINAL("final", "终面"),
    CUSTOM("custom", "自定义");

    private final String code;
    private final String description;

    public static RoundType fromCode(String code) {
        return BaseEnum.fromCode(RoundType.class, code);
    }

}
