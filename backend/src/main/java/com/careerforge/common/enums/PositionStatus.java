package com.careerforge.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 职位状态枚举
 * 用户手动设置的岗位求职状态
 *
 * @author Azir
 */
@Getter
@AllArgsConstructor
public enum PositionStatus implements BaseEnum {

    DRAFT("draft", "草稿"),
    APPLIED("applied", "已投递"),
    INTERVIEWING("interviewing", "面试中"),
    OFFERED("offered", "已获Offer"),
    REJECTED("rejected", "未通过"),
    WITHDRAWN("withdrawn", "已撤回");

    private final String code;
    private final String description;

    public static PositionStatus fromCode(String code) {
        return BaseEnum.fromCode(PositionStatus.class, code);
    }

}
