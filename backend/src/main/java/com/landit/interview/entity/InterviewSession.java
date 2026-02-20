package com.landit.interview.entity;

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
public class InterviewSession extends BaseEntity {

    private Long userId;

    private InterviewType type;

    private String position;

    private InterviewStatus status;

    private Integer currentQuestionIndex;

    private Integer totalQuestions;

}
