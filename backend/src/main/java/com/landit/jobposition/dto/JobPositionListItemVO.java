package com.landit.jobposition.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 职位列表项 VO（含面试统计）
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPositionListItemVO {

    /**
     * 职位ID
     */
    private String id;

    /**
     * 公司名称
     */
    private String companyName;

    /**
     * 职位名称
     */
    private String title;

    /**
     * 关联面试数量
     */
    private Integer interviewCount;

    /**
     * 最新面试时间
     */
    private LocalDateTime latestInterviewDate;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

}
