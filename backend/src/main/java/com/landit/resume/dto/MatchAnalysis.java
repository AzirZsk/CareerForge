package com.landit.resume.dto;

import com.landit.common.annotation.SchemaField;
import lombok.Data;

import java.util.List;

/**
 * 简历与 JD 匹配分析结果 DTO
 *
 * @author Azir
 */
@Data
public class MatchAnalysis {

    /**
     * 匹配分数 (0-100)
     */
    @SchemaField(value = "匹配分数(0-100)")
    private Integer matchScore;

    /**
     * 已匹配的技能列表
     */
    @SchemaField(value = "已匹配的技能列表")
    private List<String> matchedSkills;

    /**
     * 缺失的技能列表
     */
    @SchemaField(value = "缺失的技能列表")
    private List<String> missingSkills;

    /**
     * 相关经历列表
     */
    @SchemaField(value = "相关经历列表")
    private List<String> relevantExperiences;

    /**
     * 调整建议列表
     */
    @SchemaField(value = "调整建议列表")
    private List<String> adjustmentSuggestions;

}
