package com.landit.task.enums;

/**
 * 任务类型枚举
 *
 * @author Azir
 */
public enum TaskType {

    /**
     * 音频转录
     */
    AUDIO_TRANSCRIBE("audio_transcribe", "音频转录"),

    /**
     * 简历优化
     */
    RESUME_OPTIMIZE("resume_optimize", "简历优化"),

    /**
     * 复盘分析
     */
    REVIEW_ANALYSIS("review_analysis", "复盘分析");

    private final String code;
    private final String label;

    TaskType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static TaskType fromCode(String code) {
        for (TaskType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

}
