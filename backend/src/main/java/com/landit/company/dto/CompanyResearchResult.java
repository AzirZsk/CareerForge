package com.landit.company.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 公司调研结果 DTO
 * AI 调研公司后返回的结构化数据
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResearchResult {

    /**
     * 公司概述
     */
    private String overview;

    /**
     * 核心业务
     */
    private List<String> coreBusiness;

    /**
     * 企业文化
     */
    private String culture;

    /**
     * 技术栈
     */
    private List<String> techStack;

    /**
     * 面试特点
     */
    private List<String> interviewCharacteristics;

    /**
     * 最新动态
     */
    private List<String> recentNews;

    /**
     * 准备建议
     */
    private List<String> preparationTips;

}
