package com.careerforge.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 简历类型枚举
 *
 * @author Azir
 */
@Getter
@RequiredArgsConstructor
public enum ResumeType {

    PRIMARY("PRIMARY", "主简历"),
    DERIVED("DERIVED", "派生简历");

    @EnumValue
    private final String value;
    @JsonValue
    private final String description;

}
