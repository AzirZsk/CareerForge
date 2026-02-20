package com.landit.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 题库响应VO
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewQuestionsVO {

    private List<InterviewQuestionVO> technical;

    private List<InterviewQuestionVO> behavioral;

    /**
     * 面试题目VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterviewQuestionVO {

        private Long id;

        private String category;

        private String difficulty;

        private String question;

        private String followUp;

        private List<String> keyPoints;

        private String sampleAnswer;

    }

}
