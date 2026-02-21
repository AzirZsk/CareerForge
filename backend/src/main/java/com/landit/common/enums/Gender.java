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

    /**
     * 从文本解析性别枚举
     *
     * @param text 性别文本（男/女）
     * @return Gender枚举，未知返回null
     */
    public static Gender fromText(String text) {
        if (text == null) {
            return null;
        }
        for (Gender gender : values()) {
            if (gender.description.equals(text)) {
                return gender;
            }
        }
        return null;
    }

}
