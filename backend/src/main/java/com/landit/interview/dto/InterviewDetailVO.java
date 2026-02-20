package com.landit.interview.dto;

import com.landit.common.enums.InterviewType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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

    private Long id;

    private InterviewType type;

    private String position;

    private String company;

    private LocalDate date;

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

        private Long id;

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
