package com.landit.jobposition.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * JD分析结果 DTO
 * AI 分析JD后返回的结构化数据
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JDAnalysisResult {

    /**
     * 职位概述
     */
    private String overview;

    /**
     * 必备技能
     */
    private List<String> requiredSkills;

    /**
     * 加分技能
     */
    private List<String> plusSkills;

    /**
     * 关键关键词
     */
    private List<String> keywords;

    /**
     * 职责重点
     */
    private List<String> responsibilities;

    /**
     * 任职要求
     */
    private List<String> requirements;

    /**
     * 面试重点
     */
    private List<String> interviewFocus;

    /**
     * 准备建议
     */
    private List<String> preparationTips;

}
