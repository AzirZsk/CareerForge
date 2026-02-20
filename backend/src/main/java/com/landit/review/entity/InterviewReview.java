package com.landit.review.entity;

import com.landit.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 面试复盘实体类
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InterviewReview extends BaseEntity {

    private Long interviewId;

    private Long userId;

    private Integer overallScore;

}
