package com.careerforge.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 简历变更类型枚举
 *
 * @author Azir
 */
@Getter
@RequiredArgsConstructor
public enum ChangeType {

    MANUAL("MANUAL", "手动修改"),
    AI_OPTIMIZE("AI_OPTIMIZE", "AI优化"),
    DERIVE("DERIVE", "派生生成"),
    ROLLBACK("ROLLBACK", "版本回滚");

    @EnumValue
    private final String value;
    @JsonValue
    private final String description;

}
