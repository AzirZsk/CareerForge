package com.landit.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.landit.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 面试复盘笔记实体类
 * 存储用户手动记录的主观感受和反思
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_interview_review_note")
public class InterviewReviewNote extends BaseEntity {

    private String interviewId;

    private String overallFeeling;

    private String highPoints;

    private String weakPoints;

    private String lessonsLearned;

}
