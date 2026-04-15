package com.careerforge.interview.voice.dto;

/**
 * 语音面试默认值常量
 * 集中管理 SessionState、VoiceSessionCreateRequest、InterviewVoiceGateway 中的默认配置
 *
 * @author Azir
 */
public final class VoiceInterviewDefaults {

    private VoiceInterviewDefaults() {
    }

    /**
     * 默认面试官风格
     */
    public static final String DEFAULT_INTERVIEWER_STYLE = "professional";

    /**
     * 默认语音模式（半语音）
     */
    public static final String DEFAULT_VOICE_MODE = "half_voice";

    /**
     * 默认总问题数
     */
    public static final int DEFAULT_TOTAL_QUESTIONS = 10;

    /**
     * 默认求助次数上限
     */
    public static final int DEFAULT_ASSIST_LIMIT = 5;
}
