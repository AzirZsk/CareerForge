package com.landit.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.landit.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 面试分析实体类
 * 存储 AI 生成的结构化改进建议
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_interview_ai_analysis")
public class InterviewAIAnalysis extends BaseEntity {

    private String interviewId;

    private String adviceList;

}
