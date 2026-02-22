package com.landit.review.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.landit.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 问题分析实体类
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_question_analysis", autoResultMap = true)
public class QuestionAnalysis extends BaseEntity {

    private String reviewId;

    private String question;

    private String yourAnswer;

    private Integer score;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> keyPointsCovered;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> keyPointsMissed;

    private String suggestion;

}
