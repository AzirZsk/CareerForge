package com.landit.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 简历状态枚举
 *
 * @author Azir
 */
@Getter
@RequiredArgsConstructor
public enum ResumeStatus {

    OPTIMIZED("optimized", "已优化"),
    DRAFT("draft", "草稿");

    @EnumValue
    private final String value;
    @JsonValue
    private final String description;

}
