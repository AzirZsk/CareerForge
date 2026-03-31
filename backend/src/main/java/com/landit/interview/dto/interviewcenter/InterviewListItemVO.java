package com.landit.interview.dto.interviewcenter;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 面试列表项 VO
 *
 * @author Azir
 */
@Data
public class InterviewListItemVO {

    private String id;

    private String source;

    private String companyName;

    private String position;

    private LocalDateTime interviewDate;

    private String status;

    private String overallResult;

    private Integer roundCount;

    private Integer completedRounds;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
