package com.landit.resume.dto;

import com.landit.common.enums.ResumeStatus;
import com.landit.resume.entity.ResumeSection;
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

    private List<ResumeSectionVO> sections;

    private Integer overallScore;

    private Integer formatScore;

    private Integer contentScore;

    /**
     * 是否已完成分析
     * true: 已完成AI结构化分析
     * false: 仅有原始文本，未完成分析
     */
    private Boolean analyzed;

    /**
     * 简历模块VO
     * 支持两种模式：
     * - 单条模式：使用 content 字段（BASIC_INFO、SKILLS）
     * - 聚合模式：使用 items 字段（PROJECT、WORK、EDUCATION、CERTIFICATE）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumeSectionVO {

        private String id;

        private String type;

        private String title;

        /**
         * 单条内容（用于 BASIC_INFO、SKILLS 等单条类型）
         */
        private Object content;

        /**
         * 聚合项列表（用于 PROJECT、WORK、EDUCATION、CERTIFICATE 等多条类型）
         */
        private List<ResumeSectionItemVO> items;

        private Integer score;

        private List<ResumeSuggestionItemVO> suggestions;

    }

    /**
     * 聚合模块中的单项VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumeSectionItemVO {

        private String id;

        private String title;

        /**
         * 模块内容，类型根据模块类型不同而不同
         * @see com.landit.common.enums.SectionType
         */
        private Object content;

        /**
         * 模块评分（0-100），null表示未评分
         */
        private Integer score;

    }

    /**
     * 简历建议项VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumeSuggestionItemVO {

        private String type;

        private String content;

    }

}
