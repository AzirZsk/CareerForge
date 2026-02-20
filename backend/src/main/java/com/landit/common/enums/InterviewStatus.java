package com.landit.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 面试状态枚举
 *
 * @author Azir
 */
@Getter
@RequiredArgsConstructor
public enum InterviewStatus {

    COMPLETED("completed", "已完成"),
    IN_PROGRESS("in_progress", "进行中");

    @EnumValue
    private final String value;
    @JsonValue
    private final String description;

}
