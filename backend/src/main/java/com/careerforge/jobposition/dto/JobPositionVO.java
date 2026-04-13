package com.careerforge.jobposition.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 职位 VO
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPositionVO {

    /**
     * 职位ID
     */
    private String id;

    /**
     * 关联公司ID
     */
    private String companyId;

    /**
     * 职位名称
     */
    private String title;

    /**
     * JD原文
     */
    private String jdContent;

    /**
     * JD分析结果（JSON格式）
     */
    private String jdAnalysis;

    /**
     * 分析更新时间
     */
    private LocalDateTime jdAnalysisUpdatedAt;

}
