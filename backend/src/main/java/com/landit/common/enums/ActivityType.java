package com.landit.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 活动类型枚举
 *
 * @author Azir
 */
@Getter
@RequiredArgsConstructor
public enum ActivityType {

    INTERVIEW("interview", "面试活动"),
    RESUME("resume", "简历活动"),
    PRACTICE("practice", "练习活动"),
    REVIEW("review", "复盘活动");

    @EnumValue
    private final String value;
    @JsonValue
    private final String description;

}
