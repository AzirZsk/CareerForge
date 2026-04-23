package com.careerforge.statistics.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.careerforge.common.enums.InterviewSource;
import com.careerforge.common.enums.InterviewStatus;
import com.careerforge.interview.entity.Interview;
import com.careerforge.interview.entity.InterviewPreparation;
import com.careerforge.interview.entity.InterviewReviewNote;
import com.careerforge.interview.service.InterviewPreparationService;
import com.careerforge.interview.service.InterviewReviewNoteService;
import com.careerforge.interview.service.InterviewService;
import com.careerforge.resume.entity.Resume;
import com.careerforge.resume.service.ResumeService;
import com.careerforge.statistics.dto.StatisticsVO;
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
    private static final DateTimeFormatter WEEK_LABEL_FORMATTER = DateTimeFormatter.ofPattern("M/d");

    /**
     * 获取统计数据
     * 涉及：跨表聚合查询（面试、简历、复盘）
     *
     * @return 统计数据
     */
    private static final DateTimeFormatter DAY_LABEL_FORMATTER = DateTimeFormatter.ofPattern("M/d");

    @Transactional(readOnly = true)
    public StatisticsVO getStatistics() {
        log.info("获取统计数据");
        // 1. 获取统计概览
        StatisticsVO.StatisticsOverviewVO overview = buildOverview();
        // 2. 自适应粒度获取进度数据
        String granularity = determineGranularity();
        List<StatisticsVO.WeeklyProgressVO> progress = switch (granularity) {
            case "session" -> buildSessionProgress();
            case "day" -> buildDailyProgress();
            default -> buildWeeklyProgress();
        };
        // 3. 获取最近活动（综合多表）
        List<StatisticsVO.RecentActivityVO> recentActivity = buildRecentActivity();
        return StatisticsVO.builder()
                .overview(overview)
                .weeklyProgress(progress)
                .recentActivity(recentActivity)
                .progressGranularity(granularity)
                .build();
    }

    /**
     * 构建统计概览
     */
    private StatisticsVO.StatisticsOverviewVO buildOverview() {
        // 真实面试次数（只统计已完成的）
        LambdaQueryWrapper<Interview> realWrapper = new LambdaQueryWrapper<>();
        realWrapper.eq(Interview::getSource, InterviewSource.REAL.getCode())
                .eq(Interview::getStatus, InterviewStatus.COMPLETED.getValue());
        long realInterviews = interviewService.count(realWrapper);

        // 模拟面试次数（只统计已完成的）
        LambdaQueryWrapper<Interview> mockWrapper = new LambdaQueryWrapper<>();
        mockWrapper.eq(Interview::getSource, InterviewSource.MOCK.getCode())
                .eq(Interview::getStatus, InterviewStatus.COMPLETED.getValue());
        long mockInterviews = interviewService.count(mockWrapper);

        // 简历数量
        long resumeCount = resumeService.count();

        // 面试准备完成率
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

            // 统计该周已完成的面试数量
            LambdaQueryWrapper<Interview> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(Interview::getCreatedAt, weekStart)
                    .lt(Interview::getCreatedAt, weekEnd)
                    .eq(Interview::getStatus, InterviewStatus.COMPLETED.getValue());
            long count = interviewService.count(wrapper);

            // 计算该周模拟面试的平均分
            int avgScore = calculateWeeklyAvgScore(weekStart, weekEnd);

            String weekLabel = weekStart.format(WEEK_LABEL_FORMATTER);
            result.add(StatisticsVO.WeeklyProgressVO.builder()
                    .week(weekLabel)
                    .interviews((int) count)
                    .score(avgScore)
                    .build());
        }

        return result;
    }

    /**
     * 根据用户最早面试记录判断统计粒度
     * <= 3天: session（按次）, 4-14天: day（按天）, >14天: week（按周）
     */
    private String determineGranularity() {
        LambdaQueryWrapper<Interview> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Interview::getCreatedAt).last("LIMIT 1");
        Interview earliest = interviewService.getOne(wrapper, false);
        if (earliest == null || earliest.getCreatedAt() == null) {
            return "week";
        }
        long daysDiff = ChronoUnit.DAYS.between(earliest.getCreatedAt(), LocalDateTime.now());
        if (daysDiff <= 3) {
            return "session";
        }
        if (daysDiff <= 14) {
            return "day";
        }
        return "week";
    }

    /**
     * 按次统计进度（用于使用时长 <=3天的用户）
     * 每根柱子代表一次面试，展示最近最多10次
     */
    private List<StatisticsVO.WeeklyProgressVO> buildSessionProgress() {
        List<StatisticsVO.WeeklyProgressVO> result = new ArrayList<>();
        // 查询最近10次有分数的已完成面试（用于得分趋势tab）
        LambdaQueryWrapper<Interview> scoreWrapper = new LambdaQueryWrapper<>();
        scoreWrapper.isNotNull(Interview::getScore)
                .gt(Interview::getScore, 0)
                .eq(Interview::getStatus, InterviewStatus.COMPLETED.getValue())
                .orderByAsc(Interview::getCreatedAt)
                .last("LIMIT 10");
        List<Interview> scoredInterviews = interviewService.list(scoreWrapper);
        // 没有有效分数数据时不返回进度图表
        if (scoredInterviews.isEmpty()) {
            return result;
        }
        // 查询最近10次已完成的面试（用于面试次数tab）
        LambdaQueryWrapper<Interview> allWrapper = new LambdaQueryWrapper<>();
        allWrapper.eq(Interview::getStatus, InterviewStatus.COMPLETED.getValue())
                .orderByAsc(Interview::getCreatedAt).last("LIMIT 10");
        List<Interview> allInterviews = interviewService.list(allWrapper);
        // 取两组数据中较多的数量作为柱子数
        int count = Math.max(allInterviews.size(), scoredInterviews.size());
        // 用日期标签，方便面试次数tab也有意义
        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("M/d");
        for (int i = 0; i < count; i++) {
            String label = "";
            int score = 0;
            int interviews = 0;
            if (i < allInterviews.size()) {
                label = allInterviews.get(i).getCreatedAt().format(labelFmt);
                interviews = 1;
            }
            if (i < scoredInterviews.size()) {
                if (label.isEmpty()) {
                    label = scoredInterviews.get(i).getCreatedAt().format(labelFmt);
                }
                score = scoredInterviews.get(i).getScore();
            }
            result.add(StatisticsVO.WeeklyProgressVO.builder()
                    .week(label)
                    .score(score)
                    .interviews(interviews)
                    .build());
        }
        return result;
    }

    /**
     * 按天统计进度（用于使用时长 4-14天的用户）
     * 最多展示14天数据
     */
    private List<StatisticsVO.WeeklyProgressVO> buildDailyProgress() {
        List<StatisticsVO.WeeklyProgressVO> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        // 计算实际需要展示的天数
        LambdaQueryWrapper<Interview> earliestWrapper = new LambdaQueryWrapper<>();
        earliestWrapper.orderByAsc(Interview::getCreatedAt).last("LIMIT 1");
        Interview earliest = interviewService.getOne(earliestWrapper, false);
        int days = 7;
        if (earliest != null && earliest.getCreatedAt() != null) {
            long daysDiff = ChronoUnit.DAYS.between(earliest.getCreatedAt(), now);
            days = (int) Math.min(daysDiff + 1, 14);
        }
        for (int i = days - 1; i >= 0; i--) {
            LocalDateTime dayStart = now.minusDays(i).truncatedTo(ChronoUnit.DAYS);
            LocalDateTime dayEnd = dayStart.plusDays(1);
            // 统计该天已完成的面试数量
            LambdaQueryWrapper<Interview> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(Interview::getCreatedAt, dayStart)
                    .lt(Interview::getCreatedAt, dayEnd)
                    .eq(Interview::getStatus, InterviewStatus.COMPLETED.getValue());
            long count = interviewService.count(wrapper);
            // 计算该天模拟面试平均分
            int avgScore = calculateWeeklyAvgScore(dayStart, dayEnd);
            String dayLabel = dayStart.format(DAY_LABEL_FORMATTER);
            result.add(StatisticsVO.WeeklyProgressVO.builder()
                    .week(dayLabel)
                    .interviews((int) count)
                    .score(avgScore)
                    .build());
        }
        return result;
    }

    /**
     * 计算指定时间范围内模拟面试的平均分
     */
    private int calculateWeeklyAvgScore(LocalDateTime weekStart, LocalDateTime weekEnd) {
        LambdaQueryWrapper<Interview> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(Interview::getScore)
                .ge(Interview::getCreatedAt, weekStart)
                .lt(Interview::getCreatedAt, weekEnd)
                .isNotNull(Interview::getScore)
                .gt(Interview::getScore, 0);
        List<Interview> scoredInterviews = interviewService.list(wrapper);
        if (scoredInterviews.isEmpty()) {
            return 0;
        }
        return (int) scoredInterviews.stream()
                .mapToInt(Interview::getScore)
                .average()
                .orElse(0);
    }

    /**
     * 构建最近活动（综合面试、简历、复盘、准备事项）
     * 使用内部记录类携带原始时间戳，排序后再格式化，避免中文字符串排序不准的问题
     */
    private List<StatisticsVO.RecentActivityVO> buildRecentActivity() {
        List<ActivityEntry> entries = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 获取最近的已完成面试记录
        LambdaQueryWrapper<Interview> interviewWrapper = new LambdaQueryWrapper<>();
        interviewWrapper.eq(Interview::getStatus, InterviewStatus.COMPLETED.getValue())
                .orderByDesc(Interview::getCreatedAt)
                .last("LIMIT 5");
        List<Interview> recentInterviews = interviewService.list(interviewWrapper);

        for (Interview interview : recentInterviews) {
            boolean isReal = InterviewSource.REAL.getCode().equals(interview.getSource());
            String type = isReal ? "interview" : "practice";
            String content = isReal ? "完成面试" : "完成模拟面试";
            entries.add(new ActivityEntry(
                    type, content, interview.getScore(),
                    interview.getId().toString(), interview.getCreatedAt()
            ));
        }

        // 获取最近的简历修改
        LambdaQueryWrapper<Resume> resumeWrapper = new LambdaQueryWrapper<>();
        resumeWrapper.orderByDesc(Resume::getUpdatedAt)
                .last("LIMIT 3");
        List<Resume> recentResumes = resumeService.list(resumeWrapper);

        for (Resume resume : recentResumes) {
            entries.add(new ActivityEntry(
                    "resume",
                    "简历「" + (resume.getName() != null ? resume.getName() : "未命名") + "」更新",
                    resume.getOverallScore(),
                    resume.getId().toString(), resume.getUpdatedAt()
            ));
        }

        // 获取最近的复盘笔记
        LambdaQueryWrapper<InterviewReviewNote> reviewWrapper = new LambdaQueryWrapper<>();
        reviewWrapper.orderByDesc(InterviewReviewNote::getCreatedAt)
                .last("LIMIT 3");
        List<InterviewReviewNote> recentReviews = interviewReviewNoteService.list(reviewWrapper);

        for (InterviewReviewNote review : recentReviews) {
            entries.add(new ActivityEntry(
                    "review", "完成面试复盘", null,
                    review.getInterviewId(), review.getCreatedAt()
            ));
        }

        // 获取最近完成的准备事项
        LambdaQueryWrapper<InterviewPreparation> prepWrapper = new LambdaQueryWrapper<>();
        prepWrapper.eq(InterviewPreparation::getCompleted, true)
                .orderByDesc(InterviewPreparation::getUpdatedAt)
                .last("LIMIT 3");
        List<InterviewPreparation> recentPreps = interviewPreparationService.list(prepWrapper);

        for (InterviewPreparation prep : recentPreps) {
            entries.add(new ActivityEntry(
                    "practice", "完成准备事项：" + prep.getTitle(), null,
                    prep.getInterviewId(), prep.getUpdatedAt()
            ));
        }

        // 按原始时间倒序排序，取前6条，再格式化时间
        return entries.stream()
                .sorted(Comparator.comparing(ActivityEntry::time).reversed())
                .limit(6)
                .map(e -> StatisticsVO.RecentActivityVO.builder()
                        .type(e.type())
                        .content(e.content())
                        .time(formatTimeAgo(e.time(), now))
                        .score(e.score())
                        .relatedId(e.relatedId())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 内部记录类，携带原始时间戳用于准确排序
     */
    private record ActivityEntry(
            String type, String content, Integer score,
            String relatedId, LocalDateTime time
    ) {}

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
