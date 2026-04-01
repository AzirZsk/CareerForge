package com.landit.jobposition.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 职位详情 VO
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPositionDetailVO {

    /**
     * 职位ID
     */
    private String id;

    /**
     * 公司ID
     */
    private String companyId;

    /**
     * 公司名称
     */
    private String companyName;

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

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 关联面试列表
     */
    private List<InterviewBriefVO> interviews;

    /**
     * 面试简要信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterviewBriefVO {
        private String id;
        private String status;
        private LocalDateTime date;
        private String overallResult;
        /**
         * 面试来源（real-真实面试 / mock-模拟面试）
         */
        private String source;
        /**
         * 轮次类型（technical_1/technical_2/hr/director/cto/final/custom）
         */
        private String roundType;
        /**
         * 自定义轮次名称（仅当 roundType=custom 时有值）
         */
        private String roundName;
        /**
         * 面试类型（onsite-现场 / online-线上）
         */
        private String interviewType;
        /**
         * 现场面试地址（仅当 interviewType='onsite' 时有值）
         */
        private String location;
        /**
         * 线上面试链接（仅当 interviewType='online' 时有值）
         */
        private String onlineLink;
    }

}
