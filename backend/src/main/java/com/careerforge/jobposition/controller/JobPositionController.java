package com.careerforge.jobposition.controller;

import com.careerforge.common.response.ApiResponse;
import com.careerforge.common.response.PageResponse;
import com.careerforge.jobposition.dto.*;
import com.careerforge.jobposition.handler.JobPositionHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 职位管理控制器
 * 负责职位的 CRUD 操作
 *
 * @author Azir
 */
@Tag(name = "job-positions", description = "职位管理")
@RestController
@RequestMapping("/job-positions")
@RequiredArgsConstructor
public class JobPositionController {

    private final JobPositionHandler handler;

    @Operation(summary = "获取职位列表（含面试统计）")
    @GetMapping
    public ApiResponse<PageResponse<JobPositionListItemVO>> getJobPositionList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ApiResponse.success(handler.getJobPositionList(page, size));
    }

    @Operation(summary = "创建职位")
    @PostMapping
    public ApiResponse<JobPositionDetailVO> createJobPosition(@Valid @RequestBody CreateJobPositionRequest request) {
        return ApiResponse.success(handler.createJobPosition(request));
    }

    @Operation(summary = "获取职位详情")
    @GetMapping("/{id}")
    public ApiResponse<JobPositionDetailVO> getJobPositionDetail(@PathVariable String id) {
        return ApiResponse.success(handler.getJobPositionDetail(id));
    }

    @Operation(summary = "更新职位")
    @PutMapping("/{id}")
    public ApiResponse<JobPositionDetailVO> updateJobPosition(
            @PathVariable String id,
            @Valid @RequestBody UpdateJobPositionRequest request) {
        return ApiResponse.success(handler.updateJobPosition(id, request));
    }

    @Operation(summary = "删除职位")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteJobPosition(@PathVariable String id) {
        handler.deleteJobPosition(id);
        return ApiResponse.success();
    }

    @Operation(summary = "获取职位下的面试列表")
    @GetMapping("/{id}/interviews")
    public ApiResponse<List<JobPositionDetailVO.InterviewBriefVO>> getJobPositionInterviews(@PathVariable String id) {
        return ApiResponse.success(handler.getJobPositionInterviews(id));
    }

}
