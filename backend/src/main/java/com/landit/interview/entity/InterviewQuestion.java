package com.landit.interview.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.landit.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 面试题目实体类
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_interview_question", autoResultMap = true)
public class InterviewQuestion extends BaseEntity {

    private String type;

    private String category;

    private String difficulty;

    private String question;

    private String followUp;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> keyPoints;

    private String sampleAnswer;

}
