package com.landit.interview.dto.interviewcenter;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 面试轮次 VO
 *
 * @author Azir
 */
@Data
public class RoundVO {

    private String id;

    private String interviewId;

    private String roundType;

    private String roundName;

    private Integer roundOrder;

    private String status;

    private LocalDateTime scheduledDate;

    private LocalDateTime actualDate;

    private String notes;

    private Integer selfRating;

    private String resultNote;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
