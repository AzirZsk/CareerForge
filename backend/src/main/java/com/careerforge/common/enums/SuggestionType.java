package com.careerforge.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 建议类型枚举
 *
 * @author Azir
 */
@Getter
@RequiredArgsConstructor
public enum SuggestionType {

    CRITICAL("critical", "关键问题"),
    IMPROVEMENT("improvement", "改进建议"),
    ENHANCEMENT("enhancement", "增强建议");

    @EnumValue
    private final String value;
    @JsonValue
    private final String description;

    /**
     * 根据 value 解析枚举
     *
     * @param value 字符串值
     * @return 对应的枚举，未匹配返回 ENHANCEMENT
     */
    public static SuggestionType fromValue(String value) {
        if (value == null) {
            return ENHANCEMENT;
        }
        for (SuggestionType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return ENHANCEMENT;
    }
}
