package com.landit.jobposition.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 职位列表项 VO（含面试统计和状态推导）
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
     * 职位状态（从关联面试推导）
     * draft-草稿/applied-已投递/interviewing-面试中/offered-已获Offer/rejected-未通过/withdrawn-已撤回
     */
    private String status;

    /**
     * 下次面试时间（最近的未完成面试）
     */
    private LocalDateTime nextInterviewDate;

    /**
     * 下次面试轮次描述（如"技术二面"）
     */
    private String nextInterviewRound;

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
