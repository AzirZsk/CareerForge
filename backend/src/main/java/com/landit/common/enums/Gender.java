package com.landit.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 性别枚举
 *
 * @author Azir
 */
@Getter
@RequiredArgsConstructor
public enum Gender {

    MALE("MALE", "男"),
    FEMALE("FEMALE", "女");

    @EnumValue
    private final String value;
    @JsonValue
    private final String description;

}
