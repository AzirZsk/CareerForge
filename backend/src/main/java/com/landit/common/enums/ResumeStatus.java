package com.landit.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 简历状态枚举
 *
 * @author Azir
 */
@Getter
@RequiredArgsConstructor
public enum ResumeStatus {

    OPTIMIZED("optimized", "已优化"),
    DRAFT("draft", "草稿");

    @EnumValue
    private final String value;
    @JsonValue
    private final String description;

    /**
     * 根据 value 查找枚举
     *
     * @param value 枚举值（如 "optimized"、"draft"）
     * @return 对应枚举，未找到返回 null
     */
    public static ResumeStatus fromValue(String value) {
        for (ResumeStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        return null;
    }

}
