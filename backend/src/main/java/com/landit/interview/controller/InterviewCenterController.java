package com.landit.interview.controller;

import com.landit.common.response.ApiResponse;
import com.landit.common.response.PageResponse;
import com.landit.interview.dto.interviewcenter.*;
import com.landit.interview.handler.InterviewCenterHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 面试中心控制器
 * 负责真实面试的 CRUD 操作
 *
 * @author Azir
 */
@Tag(name = "interview-center", description = "面试中心")
@RestController
@RequestMapping("/interview-center")
@RequiredArgsConstructor
public class InterviewCenterController {

    private final InterviewCenterHandler handler;

    @Operation(summary = "创建真实面试")
    @PostMapping
    public ApiResponse<InterviewDetailVO> createInterview(@Valid @RequestBody CreateInterviewRequest request) {
        return ApiResponse.success(handler.createInterview(request));
    }

    @Operation(summary = "获取面试列表")
    @GetMapping
    public ApiResponse<PageResponse<InterviewListItemVO>> getInterviewList(
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ApiResponse.success(handler.getInterviewList(type, status, page, size));
    }

    @Operation(summary = "获取面试详情")
    @GetMapping("/{id}")
    public ApiResponse<InterviewDetailVO> getInterviewDetail(@PathVariable String id) {
        return ApiResponse.success(handler.getInterviewDetail(id));
    }

    @Operation(summary = "更新面试基本信息")
    @PutMapping("/{id}")
    public ApiResponse<InterviewDetailVO> updateInterview(
            @PathVariable String id,
            @Valid @RequestBody UpdateInterviewRequest request) {
        return ApiResponse.success(handler.updateInterview(id, request));
    }

    @Operation(summary = "删除面试")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteInterview(@PathVariable String id) {
        handler.deleteInterview(id);
        return ApiResponse.success();
    }

}
