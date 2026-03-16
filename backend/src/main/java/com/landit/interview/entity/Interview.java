package com.landit.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.landit.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 面试记录实体类
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_interview", autoResultMap = true)
public class Interview extends BaseEntity {

    private String userId;

    private String type;

    private String position;

    private String company;

    private LocalDate date;

    private Integer duration;

    private Integer score;

    private String status;

    private Integer questions;

    private Integer correctAnswers;

}
