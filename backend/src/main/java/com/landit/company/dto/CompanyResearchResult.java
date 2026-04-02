package com.landit.company.dto;

import com.landit.common.annotation.SchemaField;
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
    @SchemaField(value = "公司概述（发展历程、主营业务、行业地位）")
    private String overview;

    /**
     * 核心业务
     */
    @SchemaField(value = "核心业务列表（主要产品或服务）")
    private List<String> coreBusiness;

    /**
     * 企业文化
     */
    @SchemaField(value = "企业文化（价值观、工作氛围）")
    private String culture;

    /**
     * 技术栈
     */
    @SchemaField(value = "技术栈列表（可能使用的技术）")
    private List<String> techStack;

    /**
     * 面试特点
     */
    @SchemaField(value = "面试特点列表（面试风格、流程特点）")
    private List<String> interviewCharacteristics;

    /**
     * 最新动态
     */
    @SchemaField(value = "最新动态列表（近期新闻、发展方向）")
    private List<String> recentNews;

    /**
     * 准备建议
     */
    @SchemaField(value = "准备建议列表（针对该公司的面试准备建议）")
    private List<String> preparationTips;

}
