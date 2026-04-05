package com.landit.interview.voice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建语音面试会话响应
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceSessionCreateVO {

    /**
     * 会话 ID
     */
    private String sessionId;

    /**
     * 关联的面试 ID
     */
    private String interviewId;

    /**
     * 职位名称
     */
    private String position;

    /**
     * 语音模式
     */
    private String voiceMode;

    /**
     * 总问题数
     */
    private Integer totalQuestions;

    /**
     * 求助次数上限
     */
    private Integer assistLimit;

}
