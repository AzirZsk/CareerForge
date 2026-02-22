package com.landit.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.landit.common.entity.BaseEntity;
import com.landit.common.enums.InterviewStatus;
import com.landit.common.enums.InterviewType;
import lombok.Data;
import lombok.EqualsAndHashCode;

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

    private InterviewType type;

    private String position;

    private InterviewStatus status;

    private Integer currentQuestionIndex;

    private Integer totalQuestions;

}
