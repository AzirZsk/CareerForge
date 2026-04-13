package com.careerforge.task.enums;

/**
 * 任务状态枚举
 *
 * @author Azir
 */
public enum TaskStatus {

    /**
     * 等待中
     */
    PENDING("pending", "等待中"),

    /**
     * 执行中
     */
    RUNNING("running", "执行中"),

    /**
     * 已完成
     */
    COMPLETED("completed", "已完成"),

    /**
     * 失败
     */
    FAILED("failed", "失败");

    private final String code;
    private final String label;

    TaskStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static TaskStatus fromCode(String code) {
        for (TaskStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

}
