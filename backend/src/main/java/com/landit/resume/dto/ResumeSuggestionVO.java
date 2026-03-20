package com.landit.resume.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简历优化建议VO（独立接口用）
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeSuggestionVO {

    /**
     * 建议ID
     */
    private String id;

    /**
     * 关联的简历模块ID
     */
    private String sectionId;

    /**
     * 建议类型：critical, improvement, enhancement
     */
    private String type;

    /**
     * 建议分类（如：内容完整性、内容丰富度等）
     */
    private String category;

    /**
     * 建议标题
     */
    private String title;

    /**
     * 建议描述（详细说明）
     */
    private String description;

    /**
     * 影响程度：高、中、低
     */
    private String impact;

    /**
     * 建议位置
     */
    private String position;

}
