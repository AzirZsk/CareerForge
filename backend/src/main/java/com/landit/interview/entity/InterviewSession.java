package com.landit.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.landit.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 面试会话实体类
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_interview_session", autoResultMap = true)
public class InterviewSession extends BaseEntity {

    private String userId;

    /**
     * 关联的真实面试 ID（语音面试依附于真实面试）
     */
    private String interviewId;

    private String type;

    private String position;

    private String status;

    private Integer currentQuestionIndex;

    private Integer totalQuestions;

    /**
     * 语音模式（text-纯文本 half_voice-半语音 full_voice-全语音）
     */
    private String voiceMode;

    /**
     * 已使用求助次数
     */
    private Integer assistCount;

    /**
     * 求助次数上限
     */
    private Integer assistLimit;

    /**
     * 冻结时间点（求助时的面试暂停时间）
     */
    private LocalDateTime freezeAt;

    /**
     * 面试官风格（professional/friendly/challenging）
     */
    private String interviewerStyle;

}
