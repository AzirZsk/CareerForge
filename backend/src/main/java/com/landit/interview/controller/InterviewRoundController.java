package com.landit.interview.controller;

import com.landit.common.response.ApiResponse;
import com.landit.interview.dto.interviewcenter.*;
import com.landit.interview.handler.InterviewRoundHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 面试轮次控制器
 *
 * @author Azir
 */
@Tag(name = "interview-rounds", description = "面试轮次管理")
@RestController
@RequestMapping("/interview-center/{interviewId}/rounds")
@RequiredArgsConstructor
public class InterviewRoundController {

    private final InterviewRoundHandler handler;

    @Operation(summary = "新增面试轮次")
    @PostMapping
    public ApiResponse<RoundVO> addRound(
            @PathVariable String interviewId,
            @Valid @RequestBody AddRoundRequest request) {
        return ApiResponse.success(handler.addRound(interviewId, request));
    }

    @Operation(summary = "更新轮次信息")
    @PutMapping("/{roundId}")
    public ApiResponse<RoundVO> updateRound(
            @PathVariable String interviewId,
            @PathVariable String roundId,
            @Valid @RequestBody UpdateRoundRequest request) {
        return ApiResponse.success(handler.updateRound(interviewId, roundId, request));
    }

    @Operation(summary = "更新轮次状态")
    @PatchMapping("/{roundId}/status")
    public ApiResponse<RoundVO> updateRoundStatus(
            @PathVariable String interviewId,
            @PathVariable String roundId,
            @RequestBody UpdateRoundStatusRequest request) {
        return ApiResponse.success(handler.updateRoundStatus(interviewId, roundId, request));
    }

    @Operation(summary = "删除轮次")
    @DeleteMapping("/{roundId}")
    public ApiResponse<Void> deleteRound(
            @PathVariable String interviewId,
            @PathVariable String roundId) {
        handler.deleteRound(interviewId, roundId);
        return ApiResponse.success();
    }

    @Operation(summary = "调整轮次顺序")
    @PutMapping("/reorder")
    public ApiResponse<List<RoundVO>> reorderRounds(
            @PathVariable String interviewId,
            @RequestBody ReorderRoundsRequest request) {
        return ApiResponse.success(handler.reorderRounds(interviewId, request));
    }

}
