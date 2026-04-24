package com.careerforge.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 语音面试求助类型枚举
 *
 * @author Azir
 */
@Getter
@AllArgsConstructor
public enum AssistType implements BaseEnum {

    GIVE_HINTS("give_hints", "提示思路"),
    EXPLAIN_CONCEPT("explain_concept", "解释概念"),
    FREE_QUESTION("free_question", "自由提问");

    private final String code;
    private final String description;

    public static AssistType fromCode(String code) {
        return BaseEnum.fromCode(AssistType.class, code);
    }
}
