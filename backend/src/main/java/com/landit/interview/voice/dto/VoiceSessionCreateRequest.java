package com.landit.interview.voice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建语音面试会话请求
 *
 * @author Azir
 */
@Data
public class VoiceSessionCreateRequest {

    /**
     * 关联的真实面试 ID
     */
    @NotBlank(message = "面试ID不能为空")
    private String interviewId;

    /**
     * 总问题数（可选，默认 10）
     */
    private Integer totalQuestions = 10;

    /**
     * 求助次数上限（可选，默认 5）
     */
    private Integer assistLimit = 5;

    /**
     * 语音模式（可选，默认 half_voice）
     */
    private String voiceMode = "half_voice";

    /**
     * 面试官风格（professional/friendly/challenging）
     * 默认为专业严肃型
     */
    private String interviewerStyle = "professional";

    /**
     * 是否重新生成面试问题（默认 true）
     * 如果为 false，尝试复用同一 interviewId 下已有会话的预生成问题
     */
    private Boolean regenerateQuestions = false;

}
