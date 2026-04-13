package com.careerforge.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 对话角色枚举
 *
 * @author Azir
 */
@Getter
@RequiredArgsConstructor
public enum ConversationRole {

    INTERVIEWER("interviewer", "面试官"),
    CANDIDATE("candidate", "候选人");

    @EnumValue
    private final String value;
    @JsonValue
    private final String description;

}
