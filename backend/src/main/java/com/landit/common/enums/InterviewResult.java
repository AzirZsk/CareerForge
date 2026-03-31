package com.landit.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 面试最终结果枚举
 * 仅适用于真实面试
 *
 * @author Azir
 */
@Getter
@AllArgsConstructor
public enum InterviewResult implements BaseEnum {

    PASSED("passed", "已通过"),
    FAILED("failed", "未通过"),
    PENDING("pending", "待定");

    public static InterviewResult fromCode(String code) {
        return BaseEnum.fromCode(InterviewResult.class, code);
    }

}
