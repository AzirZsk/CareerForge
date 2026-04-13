package com.careerforge.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 性别枚举
 * Jackson 反序列化支持枚举名（MALE/FEMALE）和中文（男/女），空字符串和 null 返回 null
 * MyBatis-Plus 默认存储枚举名
 *
 * @author Azir
 */
@Getter
@RequiredArgsConstructor
public enum Gender {

    MALE("男"),
    FEMALE("女");

    private final String description;

    /**
     * Jackson 反序列化工厂方法
     * 支持枚举名（MALE/FEMALE）、中文（男/女），空字符串和 null 返回 null
     *
     * @param value JSON 中的值
     * @return Gender 枚举，无效值返回 null
     */
    @JsonCreator
    public static Gender fromValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (Gender gender : values()) {
            if (gender.name().equals(value) || gender.description.equals(value)) {
                return gender;
            }
        }
        return null;
    }

    /**
     * 从文本解析性别枚举
     *
     * @param text 性别文本（男/女/MALE/FEMALE）
     * @return Gender枚举，未知返回null
     */
    public static Gender fromText(String text) {
        return fromValue(text);
    }

}
