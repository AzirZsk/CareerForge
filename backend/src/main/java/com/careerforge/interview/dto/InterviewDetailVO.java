package com.careerforge.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 面试详情VO
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewDetailVO {

    private String id;

    private String type;

    private String position;

    private String company;

    private LocalDateTime date;

    private Integer duration;

    private Integer score;

    private List<ConversationVO> conversation;

    private InterviewAnalysisVO analysis;

    /**
     * 对话记录VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationVO {

        private String id;

        private String role;

        private String content;

        private String timestamp;

        private Integer score;

        private String feedback;

    }

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

}
