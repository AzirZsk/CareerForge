package com.landit.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 结束面试响应DTO
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinishSessionResponse {

    private Long interviewId;

    private Long reviewId;

    private Integer overallScore;

}
