package com.careerforge.interview.dto.interviewcenter;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 面试复盘笔记 VO
 * 用户手动记录的主观感受和反思
 *
 * @author Azir
 */
@Data
public class ReviewNoteVO {

    private String id;

    private String interviewId;

    private String overallFeeling;

    private String highPoints;

    private String weakPoints;

    private String lessonsLearned;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
