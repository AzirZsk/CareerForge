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

    private Long id;

    private String name;

    private String targetPosition;

    private List<ResumeSectionVO> sections;

    private Integer overallScore;

    private Integer keywordMatch;

    private Integer formatScore;

    private Integer contentScore;

    /**
     * 简历模块VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumeSectionVO {

        private Long id;

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
