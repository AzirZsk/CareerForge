package com.landit.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 面试复盘笔记类型枚举
 *
 * @author Azir
 */
@Getter
@AllArgsConstructor
public enum ReviewNoteType implements BaseEnum {

    MANUAL("manual", "手动复盘"),
    AI_ANALYSIS("ai_analysis", "AI分析复盘");

    public static ReviewNoteType fromCode(String code) {
        return BaseEnum.fromCode(ReviewNoteType.class, code);
    }

}
