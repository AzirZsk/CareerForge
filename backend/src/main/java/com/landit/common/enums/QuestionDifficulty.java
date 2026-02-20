package com.landit.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 题目难度枚举
 *
 * @author Azir
 */
@Getter
@RequiredArgsConstructor
public enum QuestionDifficulty {

    EASY("easy", "简单"),
    MEDIUM("medium", "中等"),
    HARD("hard", "困难");

    @EnumValue
    private final String value;
    @JsonValue
    private final String description;

}
