package com.landit.resume.controller;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.landit.common.response.ApiResponse;
import com.landit.common.util.JsonParseHelper;
import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.graph.ResumeOptimizeGraphService;
import com.landit.resume.handler.ResumeHandler;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    private final ResumeOptimizeGraphService graphService;
    private final ResumeHandler resumeHandler;

    /**
     * SSE 流式执行简历优化工作流
     * 简历内容从数据库读取，只需传入简历ID
     *
     * SSE 返回丰富的事件数据：
     * - 解析阶段：模块列表、完整性
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

        log.info("开始流式简历优化: resumeId={}, mode={}", id, mode);

        // 从数据库获取简历详情
        ResumeDetailVO resumeDetail = resumeHandler.getResumeDetail(id);
        if (resumeDetail == null) {
            return Flux.just(OptimizeProgressEvent.error("简历不存在", null));
        }

        // 获取目标岗位
        String position = targetPosition != null ? targetPosition : resumeDetail.getTargetPosition();
        if (position == null || position.isEmpty()) {
            position = "未知岗位";
        }

        // 将简历内容转为JSON
        String resumeContent = toJsonString(resumeDetail);

        String threadId = UUID.randomUUID().toString();

        // 构建初始状态
        Map<String, Object> initialState = new HashMap<>();
        initialState.put("resume_id", id);
        initialState.put("resume_content", resumeContent);
        initialState.put("target_position", position);
        initialState.put("diagnosis_mode", mode);
        initialState.put("messages", new java.util.ArrayList<String>());

        // 发送开始事件
        return Flux.concat(
                Flux.just(OptimizeProgressEvent.start(id, threadId, position, mode)),
                graphService.streamOptimize(initialState, threadId)
                        .map(output -> {
                            // 从状态中获取节点输出数据
                            Map<String, Object> nodeOutput = graphService.getNodeOutput(threadId);
                            return OptimizeProgressEvent.fromNodeOutput(output.node(), threadId, nodeOutput);
                        }),
                Flux.just(OptimizeProgressEvent.complete(threadId))
        ).onErrorResume(e -> {
            log.error("工作流执行失败", e);
            return Flux.just(OptimizeProgressEvent.error(e.getMessage(), threadId));
        });
    }

    /**
     * 执行简历优化工作流（同步）
     */
    @Operation(summary = "执行简历优化", description = "同步执行，返回完整结果")
    @PostMapping("/{id}/optimize")
    public ApiResponse<Map<String, Object>> executeOptimize(
            @PathVariable String id,
            @RequestBody(required = false) OptimizeRequest request) {

        log.info("执行简历优化: resumeId={}", id);

        // 从数据库获取简历
        ResumeDetailVO resumeDetail = resumeHandler.getResumeDetail(id);
        if (resumeDetail == null) {
            return ApiResponse.error(404, "简历不存在");
        }

        String mode = request != null && request.getMode() != null ? request.getMode() : "quick";
        String position = request != null && request.getTargetPosition() != null
                ? request.getTargetPosition() : resumeDetail.getTargetPosition();

        if (position == null || position.isEmpty()) {
            position = "未知岗位";
        }

        String resumeContent = toJsonString(resumeDetail);
        String threadId = UUID.randomUUID().toString();

        Map<String, Object> result;
        if ("precise".equals(mode)) {
            result = graphService.executePreciseOptimize(
                    id, resumeContent, position,
                    request != null ? request.getSearchResults() : "",
                    threadId
            );
        } else {
            result = graphService.executeQuickOptimize(id, resumeContent, position, threadId);
        }

        result.put("thread_id", threadId);
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
    public ApiResponse<Map<String, Object>> submitReview(@RequestBody ReviewRequest request) {
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

    // ==================== 辅助方法 ====================

    private String toJsonString(Object obj) {
        return JsonParseHelper.toJsonString(obj);
    }

    // ==================== 事件类 ====================

    /**
     * 优化进度事件（SSE 返回格式）
     *
     * 包含丰富的优化数据：
     * - 基本信息：节点、进度、消息
     * - 节点特定数据：
     *   - parse_resume: 模块列表、完整性
     *   - diagnose_*: 评分、维度、问题
     *   - generate_suggestions: 建议列表
     *   - optimize_section: 变更详情
     *   - save_version: 版本信息
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class OptimizeProgressEvent {
        private String event;          // start / progress / complete / error
        private String nodeId;         // 当前节点
        private Integer progress;      // 进度百分比 0-100
        private String message;        // 进度消息
        private String threadId;       // 线程ID
        private Object data;           // 节点特定数据
        private long timestamp;        // 时间戳

        public static OptimizeProgressEvent start(String resumeId, String threadId, String position, String mode) {
            return new OptimizeProgressEvent("start", "start", 0,
                    "开始优化简历 - " + position + " (" + mode + "模式)",
                    threadId,
                    Map.of("resumeId", resumeId, "targetPosition", position, "mode", mode),
                    System.currentTimeMillis());
        }

        public static OptimizeProgressEvent fromNodeOutput(String nodeId, String threadId, Map<String, Object> nodeOutput) {
            if (nodeOutput == null) {
                return new OptimizeProgressEvent("progress", nodeId, 0,
                        "处理中...", threadId, null, System.currentTimeMillis());
            }

            Integer progress = (Integer) nodeOutput.getOrDefault("progress", 0);
            String message = (String) nodeOutput.getOrDefault("message", "处理中...");
            Object data = nodeOutput.get("data");

            return new OptimizeProgressEvent("progress", nodeId, progress, message, threadId, data, System.currentTimeMillis());
        }

        public static OptimizeProgressEvent complete(String threadId) {
            return new OptimizeProgressEvent("complete", "end", 100,
                    "优化完成", threadId, null, System.currentTimeMillis());
        }

        public static OptimizeProgressEvent error(String errorMsg, String threadId) {
            return new OptimizeProgressEvent("error", null, null, errorMsg, threadId, null, System.currentTimeMillis());
        }
    }

    // ==================== 请求 DTO ====================

    @lombok.Data
    public static class OptimizeRequest {
        private String mode = "quick";
        private String targetPosition;
        private String searchResults;
    }

    @lombok.Data
    public static class ReviewRequest {
        private String threadId;
        private boolean approved;
        private Map<String, Object> modifications;
    }

}
