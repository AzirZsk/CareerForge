package com.landit.resume.dto;

import com.landit.common.annotation.SchemaField;
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
    @SchemaField(value = "总体评分(0-100)", required = true)
    private Integer overallScore;

    /**
     * 各维度评分
     */
    @SchemaField(value = "各维度评分详情")
    private DimensionScores dimensionScores;

    /**
     * 优势列表
     */
    @SchemaField(value = "简历优势列表")
    private List<String> strengths;

    /**
     * 劣势列表
     */
    @SchemaField(value = "简历劣势列表")
    private List<String> weaknesses;

    /**
     * 精准模式额外信息（仅当preciseMode=true时返回）
     */
    @SchemaField(value = "精准模式额外分析信息")
    private PreciseAnalysis preciseAnalysis;

    /**
     * 各模块评分
     * Key: 模块ID（对应简历中的 sectionId）
     * Value: 评分 (0-100)
     */
    @SchemaField(value = "各模块评分详情")
    private Map<String, Integer> sectionScores;

    /**
     * 维度评分
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionScores {
        /**
         * 内容质量评分
         */
        @SchemaField(value = "内容质量评分(0-100)")
        private Integer content;

        /**
         * 结构规范评分
         */
        @SchemaField(value = "结构规范评分(0-100)")
        private Integer structure;

        /**
         * 岗位匹配评分
         */
        @SchemaField(value = "岗位匹配评分(0-100)")
        private Integer matching;

        /**
         * 竞争力评分
         */
        @SchemaField(value = "竞争力评分(0-100)")
        private Integer competitiveness;
    }

    /**
     * 优化建议
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Suggestion {
        /**
         * 建议类型：critical（关键问题）/improvement（改进建议）/enhancement（增强建议）
         */
        @SchemaField(value = "建议类型", enumValues = {"critical", "improvement", "enhancement"})
        private String type;

        /**
         * 影响程度：high/medium/low
         */
        @SchemaField(value = "影响程度", enumValues = {"high", "medium", "low"})
        private String impact;

        /**
         * 分类：basicInfo/education/work/projects/skills/certificates/openSource/customSections
         */
        @SchemaField(value = "建议分类", enumValues = {"basicInfo", "education", "work", "projects", "skills", "certificates", "openSource", "customSections"})
        private String category;

        /**
         * 关联的简历模块ID（使用简短标识符，如 work_1, project_2）
         */
        @SchemaField(value = "建议对应的简历模块ID")
        private String sectionId;

        /**
         * 位置标识
         */
        @SchemaField(value = "建议对应的简历位置标识")
        private String position;

        /**
         * 建议标题
         */
        @SchemaField(value = "建议标题", required = true)
        private String title;

        /**
         * 当前问题描述
         */
        @SchemaField(value = "当前问题的具体描述")
        private String current;

        /**
         * 改进建议
         */
        @SchemaField(value = "具体改进建议", required = true)
        private String suggestion;

        /**
         * 对求职的实际价值
         */
        @SchemaField(value = "对求职的实际价值")
        private String value;
    }

    /**
     * 精准分析结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreciseAnalysis {
        /**
         * 市场岗位要求
         */
        @SchemaField(value = "市场岗位要求分析")
        private MarketRequirements marketRequirements;

        /**
         * 匹配分析
         */
        @SchemaField(value = "技能匹配分析")
        private MatchAnalysis matchAnalysis;

        /**
         * 匹配详情
         */
        @SchemaField(value = "各技能的详细匹配情况")
        private Map<String, MatchDetail> matchDetails;

        /**
         * 市场洞察
         */
        @SchemaField(value = "市场洞察与建议")
        private String marketInsight;
    }

    /**
     * 市场岗位要求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketRequirements {
        @SchemaField(value = "核心技能要求列表")
        private List<String> coreSkills;

        @SchemaField(value = "加分技能列表")
        private List<String> bonusSkills;

        @SchemaField(value = "常见关键词列表")
        private List<String> commonKeywords;
    }

    /**
     * 匹配分析
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchAnalysis {
        @SchemaField(value = "已匹配的技能列表")
        private List<String> matched;

        @SchemaField(value = "缺失的技能列表")
        private List<String> missing;

        @SchemaField(value = "部分匹配的技能列表")
        private List<String> partialMatch;
    }

    /**
     * 匹配详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchDetail {
        @SchemaField(value = "匹配状态", enumValues = {"matched", "missing", "partial"})
        private String status;

        @SchemaField(value = "简历中的相关证据")
        private String evidence;

        @SchemaField(value = "改进建议")
        private String suggestion;
    }
}
