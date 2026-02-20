package com.landit.common.enums;

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

}
