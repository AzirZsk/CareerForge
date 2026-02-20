package com.landit.job.controller;

import com.landit.common.response.ApiResponse;
import com.landit.common.response.PageResponse;
import com.landit.job.entity.Job;
import com.landit.job.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 职位推荐控制器
 *
 * @author Azir
 */
@Tag(name = "jobs", description = "职位推荐")
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @Operation(summary = "获取推荐职位")
    @GetMapping("/recommendations")
    public ApiResponse<PageResponse<Job>> getJobRecommendations(@RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer size) {
        return ApiResponse.success(jobService.getJobRecommendations(page, size));
    }

}
