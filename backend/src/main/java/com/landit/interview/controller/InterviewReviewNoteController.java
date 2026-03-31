package com.landit.interview.controller;

import com.landit.common.response.ApiResponse;
import com.landit.interview.dto.interviewcenter.*;
import com.landit.interview.handler.InterviewReviewNoteHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 面试复盘笔记控制器
 *
 * @author Azir
 */
@Tag(name = "interview-review-note", description = "面试复盘笔记")
@RestController
@RequestMapping("/interview-center/{interviewId}/review")
@RequiredArgsConstructor
public class InterviewReviewNoteController {

    private final InterviewReviewNoteHandler handler;

    @Operation(summary = "获取手动复盘笔记")
    @GetMapping
    public ApiResponse<ReviewNoteVO> getManualNote(@PathVariable String interviewId) {
        return ApiResponse.success(handler.getManualNote(interviewId));
    }

    @Operation(summary = "保存手动复盘笔记")
    @PostMapping
    public ApiResponse<ReviewNoteVO> saveManualNote(
            @PathVariable String interviewId,
            @Valid @RequestBody SaveReviewNoteRequest request) {
        return ApiResponse.success(handler.saveManualNote(interviewId, request));
    }

}
