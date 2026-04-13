package com.careerforge.interview.dto.interviewcenter;

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

    private String interviewType;

    private String location;

    private String onlineLink;

    private String meetingPassword;

    private String jdContent;

    private String notes;

    private String jobPositionId;

    /**
     * 关联简历ID
     */
    private String resumeId;

    /**
     * 关联简历名称（用于前端显示）
     */
    private String resumeName;

    private String companyResearch;

    private String jdAnalysis;

    private List<PreparationVO> preparations;

    /**
     * 手动复盘笔记
     */
    private ReviewNoteVO reviewNote;

    /**
     * AI 分析记录
     */
    private AIAnalysisVO aiAnalysisNote;

    /**
     * 面试过程转译文本
     */
    private String transcript;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
