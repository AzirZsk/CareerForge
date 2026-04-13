package com.landit.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 面试准备项来源枚举
 *
 * @author Azir
 */
@Getter
@AllArgsConstructor
public enum PreparationSource implements BaseEnum {

    AI_GENERATED("ai_generated", "AI生成"),
    MANUAL("manual", "手动添加");

    private final String code;
    private final String description;

    public static PreparationSource fromCode(String code) {
        return BaseEnum.fromCode(PreparationSource.class, code);
    }

}
