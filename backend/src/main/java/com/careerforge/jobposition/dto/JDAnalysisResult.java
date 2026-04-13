package com.careerforge.jobposition.dto;

import com.careerforge.common.annotation.SchemaField;
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
    @SchemaField(value = "职位概述（职责范围、核心目标）")
    private String overview;

    /**
     * 必备技能
     */
    @SchemaField(value = "必备技能列表（必须掌握的技术和技能）")
    private List<String> requiredSkills;

    /**
     * 加分技能
     */
    @SchemaField(value = "加分技能列表（优先考虑的技能）")
    private List<String> plusSkills;

    /**
     * 关键关键词
     */
    @SchemaField(value = "关键关键词列表（简历和面试中应出现的关键词）")
    private List<String> keywords;

    /**
     * 职责重点
     */
    @SchemaField(value = "职责重点列表（主要工作内容）")
    private List<String> responsibilities;

    /**
     * 任职要求
     */
    @SchemaField(value = "任职要求列表（学历、经验、软技能等）")
    private List<String> requirements;

    /**
     * 面试重点
     */
    @SchemaField(value = "面试重点列表（可能的面试问题方向）")
    private List<String> interviewFocus;

    /**
     * 准备建议
     */
    @SchemaField(value = "准备建议列表（针对性的准备建议）")
    private List<String> preparationTips;

}
