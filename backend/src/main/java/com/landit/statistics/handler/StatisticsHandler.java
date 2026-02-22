package com.landit.statistics.handler;

import com.landit.interview.service.InterviewService;
import com.landit.resume.service.ResumeService;
import com.landit.review.service.ReviewService;
import com.landit.statistics.dto.StatisticsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final ReviewService reviewService;

    /**
     * 获取统计数据
     * 涉及：跨表聚合查询（面试、简历、复盘）
     *
     * @return 统计数据
     */
    @Transactional(readOnly = true)
    public StatisticsVO getStatistics() {
        // TODO: 实现统计逻辑
        // 1. 统计面试次数
        // 2. 计算平均分
        // 3. 计算提升率
        // 4. 获取周进度数据
        // 5. 获取技能雷达图数据
        // 6. 获取最近活动
        log.info("获取统计数据");
        return null;
    }

}
