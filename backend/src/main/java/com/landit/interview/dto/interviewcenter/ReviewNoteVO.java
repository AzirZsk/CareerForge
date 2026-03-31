package com.landit.interview.dto.interviewcenter;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 面试复盘笔记 VO
 *
 * @author Azir
 */
@Data
public class ReviewNoteVO {

    private String id;

    private String interviewId;

    private String type;

    private String overallFeeling;

    private String highPoints;

    private String weakPoints;

    private String lessonsLearned;

    private String suggestions;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
