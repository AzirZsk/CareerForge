package com.landit.interview.dto.interviewcenter;

import com.landit.interview.graph.review.dto.AdviceItem;
import com.landit.interview.graph.review.dto.InterviewAnalysisResult;
import com.landit.interview.graph.review.dto.TranscriptAnalysisResult;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 面试分析 VO
 *
 * @author Azir
 */
@Data
public class AIAnalysisVO {

    private String id;

    private String interviewId;

    private List<AdviceItem> adviceList;

    /**
     * 对话分析结果
     */
    private TranscriptAnalysisResult transcriptAnalysis;

    /**
     * 面试分析结果
     */
    private InterviewAnalysisResult interviewAnalysis;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
