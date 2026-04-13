package com.careerforge.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 面试准备项优先级枚举
 *
 * @author Azir
 */
@Getter
@AllArgsConstructor
public enum PreparationPriority implements BaseEnum {

    REQUIRED("required", "必做"),
    RECOMMENDED("recommended", "推荐"),
    OPTIONAL("optional", "可选");

    private final String code;
    private final String description;

    public static PreparationPriority fromCode(String code) {
        return BaseEnum.fromCode(PreparationPriority.class, code);
    }

}
