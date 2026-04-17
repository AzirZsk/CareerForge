package com.careerforge.interview.dto.interviewcenter;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模拟面试摘要 VO
 *
 * @author Azir
 */
@Data
public class MockSessionSummaryVO {

    private String id;

    private String sessionId;

    private LocalDateTime createdAt;

    private Integer duration;

    private Integer questions;

    /**
     * AI 评分（0-100，来自 InterviewAIAnalysis）
     */
    private Integer overallScore;

    /**
     * 整体表现（优秀/良好/一般/待改进）
     */
    private String overallPerformance;

    /**
     * JD 匹配度（1-10）
     */
    private Integer jdMatchScore;

    private String interviewerStyle;

    private String voiceMode;

    private Integer assistCount;

    private Integer assistLimit;

    private Boolean hasRecording;

    private Boolean hasAnalysis;

}
