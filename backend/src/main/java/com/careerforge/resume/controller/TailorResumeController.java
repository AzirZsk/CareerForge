package com.careerforge.resume.controller;

import com.careerforge.common.response.ApiResponse;
import com.careerforge.resume.dto.ResumeDetailVO;
import com.careerforge.resume.dto.SaveTailoredResumeRequest;
import com.careerforge.resume.handler.TailorResumeGraphHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 职位适配工作流控制器
 * 提供基于 Graph 的简历定制工作流 API
 *
 * @author Azir
 */
@Slf4j
@Tag(name = "resume-tailor-workflow", description = "职位适配工作流")
@RestController
@RequestMapping("/resumes")
@RequiredArgsConstructor
public class TailorResumeController {

    private final TailorResumeGraphHandler tailorGraphHandler;

    /**
     * SSE 流式执行职位适配工作流
     * 根据目标职位和 JD 定制简历
     *
     * SSE 返回丰富的事件数据：
     * - 分析 JD 阶段：必备技能、关键词、工作职责
     * - 匹配阶段：匹配分数、已匹配/缺失技能、相关经历
     * - 定制阶段：定制后的简历内容、调整说明
     *
     * 使用 SseEmitter 确保实时推送事件
     */
    @Operation(summary = "流式执行职位适配", description = "根据目标职位和JD定制简历，实时返回进度和详细数据（SSE）")
    @GetMapping(value = "/{id}/tailor/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTailor(
            @PathVariable String id,
            @RequestParam String targetPosition,
            @RequestParam String jobDescription,
            HttpServletResponse response) {
        return tailorGraphHandler.streamTailorWithSse(id, targetPosition, jobDescription, response);
    }

    /**
     * 保存定制简历
     * 创建新的派生简历并应用定制内容
     */
    @Operation(summary = "保存定制简历", description = "创建新的派生简历并应用定制内容")
    @PostMapping("/{id}/tailor/save")
    public ApiResponse<ResumeDetailVO> saveTailored(
            @PathVariable String id,
            @Valid @RequestBody SaveTailoredResumeRequest request) {
        ResumeDetailVO result = tailorGraphHandler.saveTailoredResume(id, request);
        return ApiResponse.success(result);
    }

}
