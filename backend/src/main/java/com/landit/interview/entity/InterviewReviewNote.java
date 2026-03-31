package com.landit.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.landit.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 面试复盘笔记实体类
 * 管理面试后的复盘记录（手动 + AI分析）
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_interview_review_note")
public class InterviewReviewNote extends BaseEntity {

    private String interviewId;

    private String type;

    private String overallFeeling;

    private String highPoints;

    private String weakPoints;

    private String lessonsLearned;

    private String suggestions;

}
