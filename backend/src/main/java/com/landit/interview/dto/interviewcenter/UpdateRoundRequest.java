package com.landit.interview.dto.interviewcenter;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 更新面试轮次请求 DTO
 *
 * @author Azir
 */
@Data
public class UpdateRoundRequest {

    private String roundType;

    private String roundName;

    private LocalDateTime scheduledDate;

    private LocalDateTime actualDate;

    private String notes;

    private Integer selfRating;

    private String resultNote;

}
