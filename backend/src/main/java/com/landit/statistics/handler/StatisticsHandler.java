package com.landit.statistics.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.landit.interview.entity.Interview;
import com.landit.interview.entity.InterviewPreparation;
import com.landit.interview.entity.InterviewReviewNote;
import com.landit.interview.service.InterviewPreparationService;
import com.landit.interview.service.InterviewReviewNoteService;
import com.landit.interview.service.InterviewService;
import com.landit.resume.entity.Resume;
import com.landit.resume.service.ResumeService;
import com.landit.statistics.dto.StatisticsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 统计业务编排处理器
 * 负责处理跨表数据聚合统计
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsHandler {

    private final InterviewService interviewService;
    private final ResumeService resumeService;
    private final InterviewPreparationService interviewPreparationService;
    private final InterviewReviewNoteService interviewReviewNoteService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    /**
     * 获取统计数据
     * 涉及：跨表聚合查询（面试、简历、复盘）
     *
     * @return 统计数据
     */
    @Transactional(readOnly = true)
    public StatisticsVO getStatistics() {
        log.info("获取统计数据");

        // 1. 获取统计概览
        StatisticsVO.StatisticsOverviewVO overview = buildOverview();

        // 2. 获取周进度数据（近6周）
        List<StatisticsVO.WeeklyProgressVO> weeklyProgress = buildWeeklyProgress();

        // 3. 获取最近活动（综合多表）
        List<StatisticsVO.RecentActivityVO> recentActivity = buildRecentActivity();

        return StatisticsVO.builder()
                .overview(overview)
                .weeklyProgress(weeklyProgress)
                .recentActivity(recentActivity)
                .build();
    }

    /**
     * 构建统计概览
     */
    private StatisticsVO.StatisticsOverviewVO buildOverview() {
        // 真实面试次数
        LambdaQueryWrapper<Interview> realWrapper = new LambdaQueryWrapper<>();
        realWrapper.eq(Interview::getSource, "real");
        long realInterviews = interviewService.count(realWrapper);

        // 模拟面试次数
        LambdaQueryWrapper<Interview> mockWrapper = new LambdaQueryWrapper<>();
        mockWrapper.eq(Interview::getSource, "mock");
        long mockInterviews = interviewService.count(mockWrapper);

        // 简历数量
        long resumeCount = resumeService.count();

        // 准备完成率
        long totalPreparations = interviewPreparationService.count();
        LambdaQueryWrapper<InterviewPreparation> completedWrapper = new LambdaQueryWrapper<>();
        completedWrapper.eq(InterviewPreparation::getCompleted, true);
        long completedPreparations = interviewPreparationService.count(completedWrapper);
        int completionRate = totalPreparations > 0
                ? (int) (completedPreparations * 100 / totalPreparations)
                : 0;

        return StatisticsVO.StatisticsOverviewVO.builder()
                .realInterviews((int) realInterviews)
                .mockInterviews((int) mockInterviews)
                .resumeCount((int) resumeCount)
                .preparationCompletionRate(completionRate)
                .build();
    }

    /**
     * 构建周进度数据（近6周）
     */
    private List<StatisticsVO.WeeklyProgressVO> buildWeeklyProgress() {
        List<StatisticsVO.WeeklyProgressVO> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 5; i >= 0; i--) {
            LocalDateTime weekStart = now.minusWeeks(i).with(java.time.DayOfWeek.MONDAY).truncatedTo(ChronoUnit.DAYS);
            LocalDateTime weekEnd = weekStart.plusWeeks(1);

            LambdaQueryWrapper<Interview> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(Interview::getCreatedAt, weekStart)
                    .lt(Interview::getCreatedAt, weekEnd);
            long count = interviewService.count(wrapper);

            result.add(StatisticsVO.WeeklyProgressVO.builder()
                    .week("W" + (6 - i))
                    .interviews((int) count)
                    .score(0) // 暂不计算周平均分
                    .build());
        }

        return result;
    }

    /**
     * 构建最近活动（综合面试、简历、复盘、准备事项）
     */
    private List<StatisticsVO.RecentActivityVO> buildRecentActivity() {
        List<StatisticsVO.RecentActivityVO> activities = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 获取最近的面试记录
        LambdaQueryWrapper<Interview> interviewWrapper = new LambdaQueryWrapper<>();
        interviewWrapper.orderByDesc(Interview::getCreatedAt)
                .last("LIMIT 5");
        List<Interview> recentInterviews = interviewService.list(interviewWrapper);

        for (Interview interview : recentInterviews) {
            String type = "real".equals(interview.getSource()) ? "interview" : "practice";
            String content = "real".equals(interview.getSource())
                    ? "完成面试"
                    : "完成模拟面试";

            activities.add(StatisticsVO.RecentActivityVO.builder()
                    .type(type)
                    .content(content)
                    .time(formatTimeAgo(interview.getCreatedAt(), now))
                    .score(interview.getScore())
                    .build());
        }

        // 获取最近的简历修改
        LambdaQueryWrapper<Resume> resumeWrapper = new LambdaQueryWrapper<>();
        resumeWrapper.orderByDesc(Resume::getUpdatedAt)
                .last("LIMIT 3");
        List<Resume> recentResumes = resumeService.list(resumeWrapper);

        for (Resume resume : recentResumes) {
            activities.add(StatisticsVO.RecentActivityVO.builder()
                    .type("resume")
                    .content("简历「" + (resume.getName() != null ? resume.getName() : "未命名") + "」更新")
                    .time(formatTimeAgo(resume.getUpdatedAt(), now))
                    .score(resume.getOverallScore())
                    .build());
        }

        // 获取最近的复盘笔记
        LambdaQueryWrapper<InterviewReviewNote> reviewWrapper = new LambdaQueryWrapper<>();
        reviewWrapper.orderByDesc(InterviewReviewNote::getCreatedAt)
                .last("LIMIT 3");
        List<InterviewReviewNote> recentReviews = interviewReviewNoteService.list(reviewWrapper);

        for (InterviewReviewNote review : recentReviews) {
            activities.add(StatisticsVO.RecentActivityVO.builder()
                    .type("review")
                    .content("完成面试复盘")
                    .time(formatTimeAgo(review.getCreatedAt(), now))
                    .build());
        }

        // 获取最近完成的准备事项
        LambdaQueryWrapper<InterviewPreparation> prepWrapper = new LambdaQueryWrapper<>();
        prepWrapper.eq(InterviewPreparation::getCompleted, true)
                .orderByDesc(InterviewPreparation::getUpdatedAt)
                .last("LIMIT 3");
        List<InterviewPreparation> recentPreps = interviewPreparationService.list(prepWrapper);

        for (InterviewPreparation prep : recentPreps) {
            activities.add(StatisticsVO.RecentActivityVO.builder()
                    .type("practice")
                    .content("完成准备事项：" + prep.getTitle())
                    .time(formatTimeAgo(prep.getUpdatedAt(), now))
                    .build());
        }

        // 按时间排序（最新的在前）
        return activities.stream()
                .sorted(Comparator.comparing(StatisticsVO.RecentActivityVO::getTime))
                .limit(6)
                .collect(Collectors.toList());
    }

    /**
     * 格式化时间为"xx前"格式
     */
    private String formatTimeAgo(LocalDateTime time, LocalDateTime now) {
        if (time == null) {
            return "未知";
        }

        long minutes = ChronoUnit.MINUTES.between(time, now);
        if (minutes < 60) {
            return minutes + "分钟前";
        }

        long hours = ChronoUnit.HOURS.between(time, now);
        if (hours < 24) {
            return hours + "小时前";
        }

        long days = ChronoUnit.DAYS.between(time, now);
        if (days < 7) {
            return days + "天前";
        }

        return time.format(TIME_FORMATTER);
    }

}
