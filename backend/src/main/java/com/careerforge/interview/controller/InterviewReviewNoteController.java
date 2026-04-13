package com.careerforge.interview.controller;

import com.careerforge.common.response.ApiResponse;
import com.careerforge.interview.dto.interviewcenter.*;
import com.careerforge.interview.handler.InterviewReviewNoteHandler;
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

    @Operation(summary = "获取复盘笔记")
    @GetMapping
    public ApiResponse<ReviewNoteVO> getNote(@PathVariable String interviewId) {
        return ApiResponse.success(handler.getNote(interviewId));
    }

    @Operation(summary = "保存复盘笔记")
    @PostMapping
    public ApiResponse<ReviewNoteVO> saveNote(
            @PathVariable String interviewId,
            @Valid @RequestBody SaveReviewNoteRequest request) {
        return ApiResponse.success(handler.saveNote(interviewId, request));
    }

}
