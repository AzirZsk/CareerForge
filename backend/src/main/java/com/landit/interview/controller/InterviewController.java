package com.landit.interview.controller;

import com.landit.common.enums.InterviewType;
import com.landit.common.response.ApiResponse;
import com.landit.common.response.PageResponse;
import com.landit.interview.dto.FinishSessionRequest;
import com.landit.interview.dto.FinishSessionResponse;
import com.landit.interview.dto.HintResponse;
import com.landit.interview.dto.InterviewDetailVO;
import com.landit.interview.dto.InterviewQuestionsVO;
import com.landit.interview.dto.StartSessionRequest;
import com.landit.interview.dto.StartSessionResponse;
import com.landit.interview.dto.SubmitAnswerRequest;
import com.landit.interview.dto.SubmitAnswerResponse;
import com.landit.interview.entity.Interview;
import com.landit.interview.handler.InterviewHandler;
import com.landit.interview.service.InterviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 面试管理控制器
 * 仅负责接收 HTTP 请求和返回响应，业务逻辑由 Handler 处理
 *
 * @author Azir
 */
@Tag(name = "interviews", description = "面试管理")
@RestController
@RequestMapping("/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;
    private final InterviewHandler interviewHandler;

    @Operation(summary = "开始面试会话")
    @PostMapping("/sessions")
    public ApiResponse<StartSessionResponse> startInterviewSession(@Valid @RequestBody StartSessionRequest request) {
        return ApiResponse.success(interviewHandler.startInterviewSession(request));
    }

    @Operation(summary = "提交回答")
    @PostMapping("/sessions/{sessionId}/answers")
    public ApiResponse<SubmitAnswerResponse> submitAnswer(@PathVariable String sessionId, @Valid @RequestBody SubmitAnswerRequest request) {
        return ApiResponse.success(interviewHandler.submitAnswer(sessionId, request));
    }

    @Operation(summary = "请求提示")
    @GetMapping("/sessions/{sessionId}/hints")
    public ApiResponse<HintResponse> getHint(@PathVariable String sessionId, @RequestParam String questionId) {
        return ApiResponse.success(interviewHandler.getHint(sessionId, questionId));
    }

    @Operation(summary = "结束面试")
    @PostMapping("/sessions/{sessionId}/finish")
    public ApiResponse<FinishSessionResponse> finishInterview(@PathVariable String sessionId, @RequestBody FinishSessionRequest request) {
        return ApiResponse.success(interviewHandler.finishInterview(sessionId, request));
    }

    @Operation(summary = "获取面试历史")
    @GetMapping("/history")
    public ApiResponse<PageResponse<Interview>> getInterviewHistory(@RequestParam(required = false) InterviewType type, @RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer size) {
        return ApiResponse.success(interviewService.getInterviewHistory(type, page, size));
    }

    @Operation(summary = "获取面试详情")
    @GetMapping("/{id}")
    public ApiResponse<InterviewDetailVO> getInterviewDetail(@PathVariable String id) {
        return ApiResponse.success(interviewService.getInterviewDetail(id));
    }

    @Operation(summary = "获取题库")
    @GetMapping("/questions")
    public ApiResponse<InterviewQuestionsVO> getInterviewQuestions(@RequestParam(required = false) InterviewType type) {
        return ApiResponse.success(interviewService.getInterviewQuestions(type));
    }

}
