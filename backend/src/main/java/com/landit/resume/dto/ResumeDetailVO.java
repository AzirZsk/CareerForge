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
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumeSectionVO {

        private String id;

        private String type;

        private String title;

        private Object content;

        private Integer score;

        private List<ResumeSuggestionItemVO> suggestions;

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
