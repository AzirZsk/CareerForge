package com.landit.review.controller;

import com.landit.common.response.ApiResponse;
import com.landit.common.response.PageResponse;
import com.landit.review.dto.InterviewReviewVO;
import com.landit.review.dto.ReviewListItemVO;
import com.landit.review.handler.ReviewHandler;
import com.landit.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 复盘管理控制器
 * 仅负责接收 HTTP 请求和返回响应，业务逻辑由 Handler 处理
 *
 * @author Azir
 */
@Tag(name = "reviews", description = "复盘管理")
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewHandler reviewHandler;

    @Operation(summary = "获取复盘列表")
    @GetMapping
    public ApiResponse<PageResponse<ReviewListItemVO>> getReviews(@RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer size) {
        return ApiResponse.success(reviewService.getReviews(page, size));
    }

    @Operation(summary = "获取复盘详情")
    @GetMapping("/{id}")
    public ApiResponse<InterviewReviewVO> getReviewDetail(@PathVariable Long id) {
        return ApiResponse.success(reviewService.getReviewDetail(id));
    }

    @Operation(summary = "导出复盘报告")
    @GetMapping("/{id}/export")
    public byte[] exportReview(@PathVariable Long id) {
        return reviewHandler.exportReview(id);
    }

}
