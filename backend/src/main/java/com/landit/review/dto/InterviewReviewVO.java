package com.landit.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 面试复盘详情VO
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewReviewVO {

    private Long id;

    private Long interviewId;

    private Integer overallScore;

    private InterviewAnalysisVO analysis;

    private List<ReviewDimensionVO> dimensions;

    private List<QuestionAnalysisVO> questionAnalysis;

    private List<ImprovementPlanVO> improvementPlan;

    /**
     * 面试分析VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterviewAnalysisVO {

        private List<String> strengths;

        private List<String> weaknesses;

        private String overallFeedback;

    }

    /**
     * 复盘维度VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewDimensionVO {

        private String name;

        private Integer score;

        private Integer maxScore;

        private String feedback;

    }

    /**
     * 问题分析VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionAnalysisVO {

        private String question;

        private String yourAnswer;

        private Integer score;

        private List<String> keyPointsCovered;

        private List<String> keyPointsMissed;

        private String suggestion;

    }

    /**
     * 改进计划VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImprovementPlanVO {

        private String category;

        private List<String> items;

    }

}
