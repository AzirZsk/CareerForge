package com.landit.resume.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 简历详情VO
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeDetailVO {

    private String id;

    private String name;

    private String targetPosition;

    /**
     * 简历状态（optimized/draft）
     */
    private String status;

    /**
     * 简历评分
     */
    private Integer score;

    /**
     * 简历完整度
     */
    private Integer completeness;

    /**
     * 是否为主简历
     */
    private Boolean isPrimary;

    /**
     * 简历原文本（Markdown格式）
     */
    private String markdownContent;

    private List<ResumeSectionVO> sections;

    private Integer overallScore;

    /**
     * 内容质量评分 (0-100)
     */
    private Integer contentScore;

    /**
     * 结构规范评分 (0-100)
     */
    private Integer structureScore;

    /**
     * 岗位匹配评分 (0-100)
     */
    private Integer matchingScore;

    /**
     * 竞争力评分 (0-100)
     */
    private Integer competitivenessScore;

    /**
     * 是否已完成分析
     * true: 已完成AI结构化分析
     * false: 仅有原始文本，未完成分析
     */
    private Boolean analyzed;

    /**
     * 简历模块VO
     * 统一使用 content 字段存储内容（JSON 字符串格式）
     * - 单条类型（BASIC_INFO）：content 存储单个对象 JSON
     * - 聚合类型（WORK、PROJECT、EDUCATION 等）：content 存储数组 JSON
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumeSectionVO {

        private String id;

        /**
         * 所属简历ID
         */
        private String resumeId;

        private String type;

        private String title;

        /**
         * 模块内容（JSON 字符串格式）
         * - BASIC_INFO: 单个对象 JSON
         * - 其他聚合类型: 数组 JSON
         */
        private String content;

        private Integer score;

        private List<ResumeSuggestionItemVO> suggestions;

    }

    /**
     * 简历建议项VO
     * 关联到 t_resume_suggestion 表
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumeSuggestionItemVO {

        /**
         * 建议ID
         */
        private String id;

        /**
         * 建议类型：critical, improvement, enhancement
         */
        private String type;

        /**
         * 建议分类（如：内容完整性、内容丰富度等）
         */
        private String category;

        /**
         * 建议标题
         */
        private String title;

        /**
         * 建议描述（详细说明）
         */
        private String description;

        /**
         * 影响程度：高、中、低
         */
        private String impact;

    }

}
