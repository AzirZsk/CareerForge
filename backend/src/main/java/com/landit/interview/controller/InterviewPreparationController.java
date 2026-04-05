package com.landit.interview.controller;

import com.landit.common.response.ApiResponse;
import com.landit.interview.dto.interviewcenter.*;
import com.landit.interview.handler.InterviewPreparationHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 面试准备事项控制器
 *
 * @author Azir
 */
@Tag(name = "interview-preparation", description = "面试准备管理")
@RestController
@RequestMapping("/interview-center/{interviewId}/preparations")
@RequiredArgsConstructor
public class InterviewPreparationController {

    private final InterviewPreparationHandler handler;

    @Operation(summary = "获取准备清单")
    @GetMapping
    public ApiResponse<List<PreparationVO>> getPreparations(@PathVariable String interviewId) {
        return ApiResponse.success(handler.getPreparations(interviewId));
    }

    @Operation(summary = "添加准备事项")
    @PostMapping
    public ApiResponse<PreparationVO> addPreparation(
            @PathVariable String interviewId,
            @Valid @RequestBody AddPreparationRequest request) {
        return ApiResponse.success(handler.addPreparation(interviewId, request));
    }

    @Operation(summary = "批量添加准备事项")
    @PostMapping("/batch")
    public ApiResponse<List<PreparationVO>> batchAddPreparations(
            @PathVariable String interviewId,
            @Valid @RequestBody BatchAddPreparationRequest request) {
        return ApiResponse.success(handler.batchAddPreparations(interviewId, request));
    }

    @Operation(summary = "更新准备事项")
    @PutMapping("/{preparationId}")
    public ApiResponse<PreparationVO> updatePreparation(
            @PathVariable String interviewId,
            @PathVariable String preparationId,
            @RequestBody UpdatePreparationRequest request) {
        return ApiResponse.success(handler.updatePreparation(interviewId, preparationId, request));
    }

    @Operation(summary = "切换完成状态")
    @PatchMapping("/{preparationId}/toggle")
    public ApiResponse<PreparationVO> toggleComplete(
            @PathVariable String interviewId,
            @PathVariable String preparationId) {
        return ApiResponse.success(handler.toggleComplete(interviewId, preparationId));
    }

    @Operation(summary = "删除准备事项")
    @DeleteMapping("/{preparationId}")
    public ApiResponse<Void> deletePreparation(
            @PathVariable String interviewId,
            @PathVariable String preparationId) {
        handler.deletePreparation(interviewId, preparationId);
        return ApiResponse.success();
    }

}
