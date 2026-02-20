package com.landit.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 面试类型枚举
 *
 * @author Azir
 */
@Getter
@RequiredArgsConstructor
public enum InterviewType {

    TECHNICAL("technical", "技术面试"),
    BEHAVIORAL("behavioral", "行为面试");

    @EnumValue
    private final String value;
    @JsonValue
    private final String description;

}
