package com.landit.statistics.dto;

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

    private List<SkillRadarVO> skillRadar;

    private List<RecentActivityVO> recentActivity;

    /**
     * 统计概览VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatisticsOverviewVO {

        private Integer totalInterviews;

        private Integer averageScore;

        private Integer improvementRate;

        private Integer studyHours;

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
     * 技能雷达VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillRadarVO {

        private String skill;

        private Integer score;

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

    }

}
