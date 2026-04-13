package com.careerforge.resume.controller;

import com.careerforge.common.response.ApiResponse;
import com.careerforge.resume.handler.ResumeRewriteGraphHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 简历风格改写工作流控制器
 * 提供参考简历风格改写的文件上传解析和SSE流式改写接口
 *
 * @author Azir
 */
@Slf4j
@Tag(name = "resume-rewrite-workflow", description = "简历风格改写工作流")
@RestController
@RequestMapping("/resumes")
@RequiredArgsConstructor
public class ResumeRewriteGraphController {

    private final ResumeRewriteGraphHandler rewriteGraphHandler;

    /**
     * 上传参考简历文件并解析
     * 解析成功后返回解析结果（包含 tempKey）
     */
    @Operation(summary = "上传参考简历文件并解析", description = "上传参考简历文件（PDF/Word），AI解析后返回 tempKey 用于后续改写")
    @PostMapping(value = "/{id}/rewrite/parse-reference", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> parseReference(
            @PathVariable String id,
            @RequestPart("file") MultipartFile file) {
        log.info("收到参考简历解析请求: resumeId={}, fileName={}", id, file.getOriginalFilename());
        Map<String, Object> result = rewriteGraphHandler.parseAndCacheReference(file);
        return ApiResponse.success(result);
    }

    /**
     * SSE 流式执行风格改写工作流
     */
    @Operation(summary = "流式执行风格改写（SSE）", description = "基于参考简历风格，SSE流式改写用户简历")
    @GetMapping(value = "/{id}/rewrite/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamRewrite(
            @PathVariable String id,
            @RequestParam String tempKey,
            HttpServletResponse response) {
        log.info("收到风格改写流式请求: resumeId={}, tempKey={}", id, tempKey);
        return rewriteGraphHandler.streamRewriteWithSse(id, tempKey, response);
    }
}
