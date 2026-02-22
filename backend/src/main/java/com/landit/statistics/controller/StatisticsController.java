package com.landit.statistics.controller;

import com.landit.common.response.ApiResponse;
import com.landit.statistics.dto.StatisticsVO;
import com.landit.statistics.handler.StatisticsHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统计数据控制器
 * 仅负责接收 HTTP 请求和返回响应，业务逻辑由 Handler 处理
 *
 * @author Azir
 */
@Tag(name = "statistics", description = "数据统计")
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsHandler statisticsHandler;

    @Operation(summary = "获取统计数据")
    @GetMapping
    public ApiResponse<StatisticsVO> getStatistics() {
        return ApiResponse.success(statisticsHandler.getStatistics());
    }

}
