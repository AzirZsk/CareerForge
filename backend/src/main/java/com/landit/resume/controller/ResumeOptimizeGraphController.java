package com.landit.resume.controller;

import com.landit.resume.handler.ResumeOptimizeGraphHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
     *
     * 使用 SseEmitter 确保在 Tomcat 容器上也能实时推送事件（不被缓冲）
     */
    @Operation(summary = "流式执行简历优化", description = "从数据库读取简历，实时返回优化进度和详细数据（SSE）")
    @GetMapping(value = "/{id}/optimize/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamOptimize(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "quick") String mode,
            @RequestParam(required = false) String targetPosition,
            HttpServletResponse response) {
        return graphHandler.streamOptimizeWithSse(id, mode, targetPosition, response);
    }

}
