package com.careerforge.interview.voice.enums;

import lombok.Getter;

/**
 * 面试阶段枚举
 *
 * @author Azir
 */
@Getter
public enum InterviewPhaseEnum {
    /**
     * 等待自我介绍
     */
    WAITING_SELF_INTRODUCTION("waiting_self_introduction", "等待自我介绍"),

    /**
     * 自我介绍回应中（AI对自我介绍内容做个性化回应）
     */
    SELF_INTRO_RESPONSE("self_intro_response", "自我介绍回应中"),

    /**
     * 正在提问
     */
    ASKING_QUESTIONS("asking_questions", "正在提问"),

    /**
     * 追问中
     */
    FOLLOW_UP("follow_up", "追问中"),

    /**
     * 已完成
     */
    COMPLETED("completed", "已完成");

    /**
     * 阶段代码
     */
    private final String code;

    /**
     * 阶段描述
     */
    private final String description;

    /**
     * 构造函数
     *
     * @param code        阶段代码
     * @param description 阶段描述
     */
    InterviewPhaseEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据代码获取枚举值
     *
     * @param code 阶段代码
     * @return 枚举值，未找到返回默认值 WAITING_SELF_INTRODUCTION
     */
    public static InterviewPhaseEnum fromCode(String code) {
        for (InterviewPhaseEnum phase : values()) {
            if (phase.code.equals(code)) {
                return phase;
            }
        }
        return WAITING_SELF_INTRODUCTION;
    }
}
