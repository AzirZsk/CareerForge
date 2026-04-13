package com.landit.resume.dto;

import com.landit.common.annotation.SchemaField;
import lombok.Data;

import java.util.List;

/**
 * JD 分析结果 DTO
 * 从职位描述中提取的关键信息
 *
 * @author Azir
 */
@Data
public class JobRequirements {

    /**
     * 必备技能列表
     */
    @SchemaField(value = "必备技能列表")
    private List<String> requiredSkills;

    /**
     * 优先技能列表
     */
    @SchemaField(value = "优先技能列表")
    private List<String> preferredSkills;

    /**
     * 关键词列表
     */
    @SchemaField(value = "关键词列表")
    private List<String> keywords;

    /**
     * 工作职责列表
     */
    @SchemaField(value = "工作职责列表")
    private List<String> responsibilities;

    /**
     * 资历级别（如：初级、中级、高级、资深、专家）
     */
    @SchemaField(value = "资历级别（如：初级、中级、高级、资深、专家）")
    private String seniorityLevel;

    /**
     * 行业领域
     */
    @SchemaField(value = "行业领域")
    private String industryDomain;

}
