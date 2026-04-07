package com.landit.interview.graph.review.dto;

import com.landit.common.annotation.SchemaField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 面试分析结果 DTO
 * AI 分析面试表现后返回的结构化数据
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewAnalysisResult {

    /**
     * JD匹配度评分(1-10)
     */
    @SchemaField(value = "JD匹配度评分(1-10)")
    private Integer jdMatchScore;

    /**
     * JD匹配详情列表
     */
    @SchemaField(value = "JD匹配详情列表")
    private List<JdMatchDetail> jdMatchDetails;

    /**
     * 整体表现(优秀/良好/一般/待改进)
     */
    @SchemaField(value = "整体表现(优秀/良好/一般/待改进)")
    private String overallPerformance;

    /**
     * 整体评分(0-100)
     */
    @SchemaField(value = "整体评分(0-100)")
    private Integer overallScore;

    /**
     * 优势亮点列表
     */
    @SchemaField(value = "优势亮点列表")
    private List<String> strengths;

    /**
     * 不足之处列表
     */
    @SchemaField(value = "不足之处列表")
    private List<String> weaknesses;

    /**
     * 技能差距分析列表
     */
    @SchemaField(value = "技能差距分析列表")
    private List<SkillGap> skillGaps;

    /**
     * 简历一致性分析
     */
    @SchemaField(value = "简历一致性分析")
    private ResumeConsistency resumeConsistency;

    /**
     * 轮次分析
     */
    @SchemaField(value = "轮次分析")
    private RoundAnalysis roundAnalysis;

    /**
     * 综合总结
     */
    @SchemaField(value = "综合总结")
    private String summary;

    /**
     * JD匹配详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JdMatchDetail {
        /**
         * 技能名称
         */
        @SchemaField(value = "技能名称")
        private String skill;

        /**
         * 是否为必备技能
         */
        @SchemaField(value = "是否为必备技能")
        private Boolean required;

        /**
         * 匹配级别(strong/medium/weak/missing)
         */
        @SchemaField(value = "匹配级别(strong/medium/weak/missing)")
        private String matchLevel;

        /**
         * 面试中的相关证据
         */
        @SchemaField(value = "面试中的相关证据")
        private String evidence;
    }

    /**
     * 技能差距
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillGap {
        /**
         * 缺失技能
         */
        @SchemaField(value = "缺失技能")
        private String skill;

        /**
         * JD中的要求描述
         */
        @SchemaField(value = "JD中的要求描述")
        private String jdRequirement;

        /**
         * 当前水平评估
         */
        @SchemaField(value = "当前水平评估")
        private String currentLevel;

        /**
         * 差距说明
         */
        @SchemaField(value = "差距说明")
        private String gapDescription;
    }

    /**
     * 简历一致性分析
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumeConsistency {
        /**
         * 一致性评分(1-5)
         */
        @SchemaField(value = "一致性评分(1-5)")
        private Integer score;

        /**
         * 一致性发现列表
         */
        @SchemaField(value = "一致性发现列表")
        private List<String> findings;
    }

    /**
     * 轮次分析
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoundAnalysis {
        /**
         * 轮次类型
         */
        @SchemaField(value = "轮次类型")
        private String roundType;

        /**
         * 表现评价
         */
        @SchemaField(value = "表现评价")
        private String performance;
    }
}
