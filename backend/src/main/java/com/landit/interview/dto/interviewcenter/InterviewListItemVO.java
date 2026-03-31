package com.landit.interview.dto.interviewcenter;

import lombok.Data;

import java.time.LocalDate;

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

    private LocalDate interviewDate;

    private String status;

    private String overallResult;

    private Integer roundCount;

    private Integer completedRounds;

    private LocalDate createdAt;

    private LocalDate updatedAt;

}
