package com.careerforge.statistics.service;

import org.springframework.stereotype.Service;

/**
 * 统计服务实现类
 * 统计服务不继承 ServiceImpl，因为统计是跨表聚合操作
 * 所有统计逻辑由 StatisticsHandler 处理
 *
 * @author Azir
 */
@Service
public class StatisticsService {

    // 统计服务暂无独立方法，所有逻辑由 Handler 处理

}
