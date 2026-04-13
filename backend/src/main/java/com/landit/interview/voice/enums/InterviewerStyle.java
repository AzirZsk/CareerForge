package com.landit.interview.voice.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 面试官风格枚举
 *
 * @author Azir
 */
@Getter
@RequiredArgsConstructor
public enum InterviewerStyle {
    /**
     * 专业严肃型 - 像大厂面试官，严谨专业，追问深入
     */
    PROFESSIONAL("professional", "专业严肃型"),

    /**
     * 亲和引导型 - 像导师面试，温和引导，给予鼓励
     */
    FRIENDLY("friendly", "亲和引导型"),

    /**
     * 压力挑战型 - 像压力面试，刻意追问，测试应变能力
     */
    CHALLENGING("challenging", "压力挑战型");

    private final String code;
    private final String description;

    /**
     * 根据 code 获取枚举
     */
    public static InterviewerStyle fromCode(String code) {
        if (code == null) {
            return PROFESSIONAL;
        }
        for (InterviewerStyle style : values()) {
            if (style.getCode().equals(code)) {
                return style;
            }
        }
        return PROFESSIONAL;
    }
}
