package com.careerforge.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 面试轮次状态枚举
 *
 * @author Azir
 */
@Getter
@AllArgsConstructor
public enum RoundStatus implements BaseEnum {

    PENDING("pending", "待面试"),
    IN_PROGRESS("in_progress", "进行中"),
    PASSED("passed", "已通过"),
    FAILED("failed", "未通过"),
    PENDING_RESULT("pending_result", "待定"),
    CANCELLED("cancelled", "已取消");

    private final String code;
    private final String description;

    public boolean isTerminal() {
        return this == PASSED || this == FAILED || this == CANCELLED;
    }

    public boolean canTransitionTo(RoundStatus target) {
        if (isTerminal()) {
            return false;
        }
        if (this == PENDING) {
            return target == IN_PROGRESS || target == CANCELLED;
        }
        if (this == IN_PROGRESS) {
            return target == PASSED || target == FAILED || target == PENDING_RESULT;
        }
        if (this == PENDING_RESULT) {
            return target == PASSED || target == FAILED;
        }
        return false;
    }

    public static RoundStatus fromCode(String code) {
        return BaseEnum.fromCode(RoundStatus.class, code);
    }

}
