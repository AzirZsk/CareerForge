package com.careerforge.resume.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 简历建议分组响应 VO
 * 按简历分组展示优化建议
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeSuggestionsGroupVO {

    /**
     * 简历ID
     */
    private String resumeId;

    /**
     * 简历名称
     */
    private String resumeName;

    /**
     * 目标岗位
     */
    private String targetPosition;

    /**
     * 建议总数
     */
    private Integer suggestionCount;

    /**
     * 建议列表（按优先级排序，最多3条）
     */
    private List<ResumeSuggestionVO> suggestions;
}
