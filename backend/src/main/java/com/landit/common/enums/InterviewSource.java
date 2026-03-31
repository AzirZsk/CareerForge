package com.landit.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 面试来源枚举
 * 区分真实面试和模拟面试
 *
 * @author Azir
 */
@Getter
@AllArgsConstructor
public enum InterviewSource implements BaseEnum {

    REAL("real", "真实面试"),
    MOCK("mock", "模拟面试");

    public static InterviewSource fromCode(String code) {
        return BaseEnum.fromCode(InterviewSource.class, code);
    }

}
