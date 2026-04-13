package com.careerforge.interview.voice.enums;

/**
 * 转录角色枚举
 * 统一管理角色字符串值
 *
 * @author Azir
 */
public enum TranscriptRole {

    /**
     * 面试官
     */
    INTERVIEWER("interviewer"),

    /**
     * 候选人
     */
    CANDIDATE("candidate"),

    /**
     * 助手
     */
    ASSISTANT("assistant");

    private final String value;

    TranscriptRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 根据字符串值获取枚举
     */
    public static TranscriptRole fromValue(String value) {
        for (TranscriptRole role : TranscriptRole.values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown transcript role: " + value);
    }

    /**
     * 验证字符串值是否有效
     */
    public static boolean isValid(String value) {
        for (TranscriptRole role : TranscriptRole.values()) {
            if (role.value.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
