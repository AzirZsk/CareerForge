package com.careerforge.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统计数据VO
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsVO {

    private StatisticsOverviewVO overview;

    private List<WeeklyProgressVO> weeklyProgress;

    private List<RecentActivityVO> recentActivity;

    // 进度图表统计粒度: "session" | "day" | "week"
    private String progressGranularity;

    /**
     * 统计概览VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatisticsOverviewVO {

        /**
         * 真实面试次数
         */
        private Integer realInterviews;

        /**
         * 模拟面试次数
         */
        private Integer mockInterviews;

        /**
         * 简历数量
         */
        private Integer resumeCount;

        /**
         * 面试准备完成率（百分比 0-100）
         */
        private Integer preparationCompletionRate;

    }

    /**
     * 周进度VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyProgressVO {

        private String week;

        private Integer score;

        private Integer interviews;

    }

    /**
     * 最近活动VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivityVO {

        private String type;

        private String content;

        private String time;

        private Integer score;

        /**
         * 关联实体ID，用于前端跳转详情页
         */
        private String relatedId;

    }

}
