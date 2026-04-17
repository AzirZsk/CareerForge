package com.careerforge.interview.voice.dto;

import com.careerforge.interview.voice.enums.InterviewPhaseEnum;
import lombok.Data;

/**
 * 语音面试会话状态
 * 用于存储会话运行时状态
 *
 * @author Azir
 */
@Data
public class SessionState {
    /**
     * 关联的真实面试ID
     */
    private String interviewId;

    /**
     * 面试职位名称
     */
    private String position;

    /**
     * 职位描述（JD）内容
     */
    private String jdContent;

    /**
     * 候选人简历内容
     */
    private String resumeContent;

    /**
     * 面试官风格（如专业、友好、严格等）
     */
    private String interviewerStyle = VoiceInterviewDefaults.DEFAULT_INTERVIEWER_STYLE;

    /**
     * 语音模式：half_voice（候选人语音/AI文字）、full_voice（双方语音）
     */
    private String voiceMode = VoiceInterviewDefaults.DEFAULT_VOICE_MODE;

    /**
     * 会话状态（枚举替代多布尔值，避免不一致状态）
     */
    private InterviewSessionState sessionState = InterviewSessionState.IDLE;

    /**
     * 面试开始时间戳（毫秒）
     */
    private long startTime;

    /**
     * 面试冻结时间戳（毫秒），用于暂停计时
     */
    private long freezeTime;

    /**
     * 当前问题序号（从0开始）
     */
    private int currentQuestion = 0;

    /**
     * 问题总数（创建会话时从数据库加载）
     */
    private int totalQuestions = VoiceInterviewDefaults.DEFAULT_TOTAL_QUESTIONS;

    /**
     * 剩余求助次数
     */
    private int assistRemaining = VoiceInterviewDefaults.DEFAULT_ASSIST_LIMIT;

    /**
     * 求助次数上限（创建会话时从数据库加载）
     */
    private int assistLimit = VoiceInterviewDefaults.DEFAULT_ASSIST_LIMIT;

    /**
     * 录音片段索引（递增，用于录音存储）
     */
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
     * 判断面试是否活跃（面试进行中或已冻结）
     */
    public boolean isActive() {
        return sessionState == InterviewSessionState.INTERVIEWING || sessionState == InterviewSessionState.FROZEN;
    }

    /**
     * 判断面试是否已冻结
     */
    public boolean isFrozen() {
        return sessionState == InterviewSessionState.FROZEN;
    }

    /**
     * 获取会话状态枚举
     */
    public InterviewSessionState getState() {
        return sessionState;
    }

    /**
     * 获取会话状态代码
     */
    public String getStateCode() {
        return sessionState.getCode();
    }

    /**
     * 获取已面试时长（秒）
     */
    public int getElapsedTime() {
        if (startTime == 0) return 0;
        return (int) ((System.currentTimeMillis() - startTime) / 1000);
    }

    /**
     * 标记面试开始
     */
    public void startInterview() {
        this.sessionState = InterviewSessionState.INTERVIEWING;
    }

    /**
     * 标记面试冻结
     */
    public void freeze() {
        this.sessionState = InterviewSessionState.FROZEN;
    }

    /**
     * 标记面试恢复
     */
    public void resume() {
        this.sessionState = InterviewSessionState.INTERVIEWING;
    }

    /**
     * 标记面试完成
     */
    public void complete() {
        this.sessionState = InterviewSessionState.COMPLETED;
    }

    /**
     * 标记会话不活跃（WebSocket 断开或异常）
     */
    public void deactivate() {
        this.sessionState = InterviewSessionState.IDLE;
    }
}
