package com.careerforge.interview.controller;

import com.careerforge.common.response.ApiResponse;
import com.careerforge.common.response.PageResponse;
import com.careerforge.interview.dto.interviewcenter.*;
import com.careerforge.interview.handler.InterviewCenterHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

    // ===== 工作流 SSE 端点 =====

    @Operation(summary = "流式执行面试准备工作流")
    @GetMapping("/{id}/preparation/stream")
    public SseEmitter streamPreparation(
            @PathVariable String id,
            HttpServletResponse response) {
        return handler.streamPreparation(id, response);
    }

    @Operation(summary = "流式执行复盘分析工作流")
    @PostMapping("/{id}/review-analysis/stream")
    public SseEmitter streamReviewAnalysis(
            @PathVariable String id,
            @RequestBody(required = false) ReviewAnalysisRequest request,
            HttpServletResponse response) {
        String transcript = (request != null) ? request.getTranscript() : null;
        return handler.streamReviewAnalysis(id, transcript, response);
    }

}
