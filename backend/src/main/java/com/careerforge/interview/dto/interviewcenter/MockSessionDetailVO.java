package com.careerforge.interview.dto.interviewcenter;

import lombok.Data;

/**
 * 模拟面试详情 VO
 *
 * @author Azir
 */
@Data
public class MockSessionDetailVO {

    private MockSessionSummaryVO summary;

    private AIAnalysisVO aiAnalysis;

    private String sessionId;

}
