package com.landit.resume.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 简历诊断响应DTO
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnoseResumeResponse {

    /**
     * 总体评分 (0-100)
     */
    private Integer overallScore;

    /**
     * 各维度评分
     */
    private DimensionScores dimensionScores;

    /**
     * 优化建议列表
     */
    private List<Suggestion> suggestions;

    /**
     * 优势列表
     */
    private List<String> strengths;

    /**
     * 劣势列表
     */
    private List<String> weaknesses;

    /**
     * 快速改进项
     */
    private List<String> quickWins;

    /**
     * 精准模式额外信息（仅当preciseMode=true时返回）
     */
    private PreciseAnalysis preciseAnalysis;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionScores {
        /**
         * 内容质量评分
         */
        private Integer content;

        /**
         * 结构规范评分
         */
        private Integer structure;

        /**
         * 岗位匹配评分
         */
        private Integer matching;

        /**
         * 竞争力评分
         */
        private Integer competitiveness;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Suggestion {
        /**
         * 优先级：high/medium/low
         */
        private String priority;

        /**
         * 分类：work/project/skills/education/summary
         */
        private String category;

        /**
         * 位置标识
         */
        private String position;

        /**
         * 建议标题
         */
        private String title;

        /**
         * 当前问题描述
         */
        private String current;

        /**
         * 改进建议
         */
        private String suggestion;

        /**
         * 影响说明
         */
        private String impact;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreciseAnalysis {
        /**
         * 市场岗位要求
         */
        private MarketRequirements marketRequirements;

        /**
         * 匹配分析
         */
        private MatchAnalysis matchAnalysis;

        /**
         * 匹配详情
         */
        private Map<String, MatchDetail> matchDetails;

        /**
         * 市场洞察
         */
        private String marketInsight;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketRequirements {
        private List<String> coreSkills;
        private List<String> bonusSkills;
        private List<String> commonKeywords;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchAnalysis {
        private List<String> matched;
        private List<String> missing;
        private List<String> partialMatch;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchDetail {
        private String status;
        private String evidence;
        private String suggestion;
    }

}
