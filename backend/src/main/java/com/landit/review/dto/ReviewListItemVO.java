package com.landit.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 复盘列表项VO
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewListItemVO {

    private Long id;

    private Long interviewId;

    private Integer overallScore;

    private String date;

}
