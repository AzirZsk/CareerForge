package com.careerforge.interview.graph.review.dto;

import com.careerforge.common.annotation.SchemaField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 面试对话分析结果 DTO
 * AI 分析面试对话文本后返回的结构化数据
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptAnalysisResult {

    /**
     * 问答对分析列表
     */
    @SchemaField(value = "问答对分析列表")
    private List<QAPair> qaPairs;

    /**
     * 整体清晰度平均分(1-5)
     */
    @SchemaField(value = "整体清晰度平均分(1-5)")
    private Double overallClarity;

    /**
     * 整体简历匹配度平均分(1-5)
     */
    @SchemaField(value = "整体简历匹配度平均分(1-5)")
    private Double overallResumeMatch;

    /**
     * JD匹配度评分(1-5)
     */
    @SchemaField(value = "JD匹配度评分(1-5)")
    private Integer jdMatchScore;

    /**
     * JD匹配度总体评价
     */
    @SchemaField(value = "JD匹配度总体评价")
    private String jdMatchReason;

    /**
     * 整体对话质量总结
     */
    @SchemaField(value = "整体对话质量总结")
    private String summary;

    /**
     * 问答对分析
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QAPair {
        /**
         * 面试官的原始问题
         */
        @SchemaField(value = "面试官的原始问题")
        private String question;

        /**
         * 问题意图分析
         */
        @SchemaField(value = "问题意图分析")
        private String questionIntent;

        /**
         * 考察的能力/知识点
         */
        @SchemaField(value = "考察的能力/知识点")
        private String assessmentTarget;

        /**
         * 与JD的关联
         */
        @SchemaField(value = "与JD的关联（具体技能/要求）")
        private String jdRelevance;

        /**
         * 候选人的原始回答
         */
        @SchemaField(value = "候选人的原始回答")
        private String answer;

        /**
         * 回答清晰度评分(1-5)
         */
        @SchemaField(value = "回答清晰度评分(1-5)")
        private Integer clarityScore;

        /**
         * 清晰度评分理由
         */
        @SchemaField(value = "清晰度评分理由")
        private String clarityReason;

        /**
         * 简历匹配度评分(1-5)
         */
        @SchemaField(value = "简历匹配度评分(1-5)")
        private Integer resumeMatchScore;

        /**
         * 简历匹配度理由
         */
        @SchemaField(value = "简历匹配度理由")
        private String resumeMatchReason;

        /**
         * 改进建议
         */
        @SchemaField(value = "改进建议")
        private String improvementSuggestion;
    }
}
