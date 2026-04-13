package com.careerforge.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.careerforge.common.entity.BaseEntity;
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

    /**
     * 对话分析结果（JSON格式: TranscriptAnalysisResult）
     */
    private String transcriptAnalysis;

    /**
     * 面试分析结果（JSON格式: InterviewAnalysisResult）
     */
    private String interviewAnalysis;

}
