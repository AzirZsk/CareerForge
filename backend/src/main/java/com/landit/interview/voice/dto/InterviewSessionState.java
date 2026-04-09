package com.landit.interview.voice.dto;

/**
 * 面试会话状态枚举
 *
 * @author Azir
 */
public enum InterviewSessionState {
    /**
     * 空闲状态
     */
    IDLE("idle", "空闲"),

    /**
     * 面试进行中
     */
    INTERVIEWING("interviewing", "面试进行中"),

    /**
     * 已冻结（暂停）
     */
    FROZEN("frozen", "已冻结"),

    /**
     * 已完成
     */
    COMPLETED("completed", "已完成"),

    /**
     * 未找到
     */
    NOT_FOUND("not_found", "未找到");

    private final String code;
    private final String description;

    InterviewSessionState(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据会话状态字段计算当前状态
     *
     * @param completed 是否已完成
     * @param frozen    是否已冻结
     * @param active    是否活跃
     * @return 会话状态
     */
    public static InterviewSessionState from(boolean completed, boolean frozen, boolean active) {
        if (completed) return COMPLETED;
        if (frozen) return FROZEN;
        if (active) return INTERVIEWING;
        return IDLE;
    }
}
