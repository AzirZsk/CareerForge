package com.landit.resume.controller;

import com.landit.common.response.ApiResponse;
import com.landit.resume.dto.OptimizeGraphRequest;
import com.landit.resume.dto.OptimizeProgressEvent;
import com.landit.resume.dto.ReviewGraphRequest;
import com.landit.resume.graph.ResumeOptimizeGraphService;
import com.landit.resume.handler.ResumeOptimizeGraphHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 简历优化工作流控制器
 * 提供基于 Graph 的简历优化工作流 API
 *
 * @author Azir
 */
@Slf4j
@Tag(name = "resume-optimize-workflow", description = "简历优化工作流")
@RestController
@RequestMapping("/resumes")
@RequiredArgsConstructor
public class ResumeOptimizeGraphController {

    private final ResumeOptimizeGraphHandler graphHandler;
    private final ResumeOptimizeGraphService graphService;

    /**
     * SSE 流式执行简历优化工作流
     * 简历内容从数据库读取，只需传入简历ID
     *
     * SSE 返回丰富的事件数据：
     * - 开始阶段：模块列表、完整性（合并原解析阶段数据）
     * - 诊断阶段：评分、各维度分数、问题列表
     * - 建议阶段：优化建议列表、快速改进项
     * - 优化阶段：变更详情、优化前后对比
     * - 保存阶段：版本信息、分数提升
     */
    @Operation(summary = "流式执行简历优化", description = "从数据库读取简历，实时返回优化进度和详细数据（SSE）")
    @GetMapping(value = "/{id}/optimize/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<OptimizeProgressEvent> streamOptimize(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "quick") String mode,
            @RequestParam(required = false) String targetPosition) {
        return graphHandler.streamOptimize(id, mode, targetPosition);
    }

    /**
     * 执行简历优化工作流（同步）
     */
    @Operation(summary = "执行简历优化", description = "同步执行，返回完整结果")
    @PostMapping("/{id}/optimize")
    public ApiResponse<Map<String, Object>> executeOptimize(
            @PathVariable String id,
            @RequestBody(required = false) OptimizeGraphRequest request) {
        Map<String, Object> result = graphHandler.executeOptimize(id, request);
        return ApiResponse.success(result);
    }

    /**
     * 获取工作流状态
     */
    @Operation(summary = "获取工作流状态")
    @GetMapping("/workflow/state")
    public ApiResponse<Map<String, Object>> getWorkflowState(@RequestParam String threadId) {
        return ApiResponse.success(graphService.getState(threadId));
    }

    /**
     * 提交人工审核结果
     */
    @Operation(summary = "提交人工审核")
    @PostMapping("/workflow/review")
    public ApiResponse<Map<String, Object>> submitReview(@RequestBody ReviewGraphRequest request) {
        Map<String, Object> result = graphService.submitReview(
                request.getThreadId(),
                request.isApproved(),
                request.getModifications()
        );
        return ApiResponse.success(result);
    }

    /**
     * 恢复工作流执行
     */
    @Operation(summary = "恢复工作流")
    @PostMapping("/workflow/resume")
    public ApiResponse<Map<String, Object>> resumeWorkflow(@RequestParam String threadId) {
        Map<String, Object> result = graphService.resumeOptimize(threadId);
        return ApiResponse.success(result);
    }

}
