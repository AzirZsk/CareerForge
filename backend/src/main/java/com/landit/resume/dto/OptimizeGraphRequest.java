package com.landit.resume.dto;

import lombok.Data;

/**
 * 简历优化工作流请求
 *
 * @author Azir
 */
@Data
public class OptimizeGraphRequest {
    /**
     * 优化模式：quick（快速）/ precise（精准）
     */
    private String mode = "quick";

    /**
     * 目标岗位（可选，不传则使用简历中的目标岗位）
     */
    private String targetPosition;

    /**
     * 搜索结果（精准模式使用）
     */
    private String searchResults;
}
