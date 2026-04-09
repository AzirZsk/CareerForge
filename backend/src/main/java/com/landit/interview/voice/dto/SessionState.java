package com.landit.interview.voice.dto;

import com.landit.interview.voice.enums.InterviewPhaseEnum;
import lombok.Data;

/**
 * 语音面试会话状态
 * 用于存储会话运行时状态
 *
 * @author Azir
 */
@Data
public class SessionState {
    // 关联的真实面试信息
    private String interviewId;
    private String position;
    private String jdContent;
    private String resumeContent;
    // 面试官风格
    private String interviewerStyle = "professional";
    // 语音模式（half_voice/full_voice）
    private String voiceMode = "half_voice";
    // 会话状态
    private boolean active = false;
    private boolean frozen = false;
    private boolean completed = false;
    private long startTime;
    private long freezeTime;
    private int currentQuestion = 0;
    private int totalQuestions = 10;
    private int assistRemaining = 5;
    private int assistLimit = 5;
    private int segmentIndex = 0;

    /**
     * 面试阶段
     */
    private InterviewPhaseEnum phase = InterviewPhaseEnum.WAITING_SELF_INTRODUCTION;

    /**
     * 上一个问题（用于生成追问）
     */
    private String lastQuestion;

    /**
     * 当前话题的追问次数
     */
    private int followUpCount = 0;

    /**
     * 每个问题最多追问次数
     */
    public static final int MAX_FOLLOW_UP = 2;

    /**
     * 获取会话状态枚举
     */
    public InterviewSessionState getState() {
        return InterviewSessionState.from(completed, frozen, active);
    }

    /**
     * 获取会话状态代码
     */
    public String getStateCode() {
        return getState().getCode();
    }

    /**
     * 获取已面试时长（秒）
     */
    public int getElapsedTime() {
        if (startTime == 0) return 0;
        return (int) ((System.currentTimeMillis() - startTime) / 1000);
    }
}
