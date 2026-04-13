package com.careerforge.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 提交回答响应DTO
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerResponse {

    private Integer score;

    private String feedback;

    private StartSessionResponse.SessionQuestionVO nextQuestion;

    private Boolean isFollowUp;

    private Boolean finished;

}
