package com.careerforge.interview.dto.interviewcenter;

import lombok.Data;

/**
 * 保存手动复盘请求 DTO
 *
 * @author Azir
 */
@Data
public class SaveReviewNoteRequest {

    private String overallFeeling;

    private String highPoints;

    private String weakPoints;

    private String lessonsLearned;

}
