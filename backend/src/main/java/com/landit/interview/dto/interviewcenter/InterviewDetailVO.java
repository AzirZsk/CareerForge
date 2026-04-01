package com.landit.interview.dto.interviewcenter;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 面试详情 VO
 *
 * @author Azir
 */
@Data
public class InterviewDetailVO {

    private String id;

    private String source;

    private String companyName;

    private String position;

    private LocalDateTime interviewDate;

    private String status;

    private String overallResult;

    private String roundType;

    private String roundName;

    private String jdContent;

    private String notes;

    private String jobPositionId;

    private String companyResearch;

    private String jdAnalysis;

    private List<RoundVO> rounds;

    private List<PreparationVO> preparations;

    private ReviewNoteVO reviewNote;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
