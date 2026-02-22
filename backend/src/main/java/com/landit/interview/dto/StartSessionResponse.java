package com.landit.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 开始面试会话响应DTO
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartSessionResponse {

    private String sessionId;

    private SessionQuestionVO firstQuestion;

    /**
     * 会话问题VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionQuestionVO {

        private String id;

        private String question;

        private List<String> keyPoints;

    }

}
