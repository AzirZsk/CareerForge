package com.landit.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.landit.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 面试轮次实体类
 * 管理一次面试的多轮流程
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_interview_round")
public class InterviewRound extends BaseEntity {

    private String interviewId;

    private String roundType;

    private String roundName;

    private Integer roundOrder;

    private String status;

    private LocalDateTime scheduledDate;

    private LocalDateTime actualDate;

    private String notes;

    private Integer selfRating;

    private String resultNote;

}
