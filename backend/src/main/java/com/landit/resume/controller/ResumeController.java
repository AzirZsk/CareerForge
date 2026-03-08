package com.landit.resume.controller;

import com.landit.common.response.ApiResponse;
import com.landit.resume.dto.AddSectionRequest;
import com.landit.resume.dto.PrimaryResumeVO;
import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.handler.ResumeHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 简历管理控制器
 * 仅负责接收 HTTP 请求和返回响应，业务逻辑由 Handler 处理
 *
 * @author Azir
 */
@Tag(name = "resumes", description = "简历管理")
@RestController
@RequestMapping("/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeHandler resumeHandler;

    @Operation(summary = "获取主简历")
    @GetMapping("/primary")
    public ApiResponse<PrimaryResumeVO> getPrimaryResume() {
        return ApiResponse.success(resumeHandler.getPrimaryResume());
    }

    @Operation(summary = "获取简历详情")
    @GetMapping("/{id}")
    public ApiResponse<ResumeDetailVO> getResumeDetail(@PathVariable String id) {
        return ApiResponse.success(resumeHandler.getResumeDetail(id));
    }

    @Operation(summary = "更新简历模块")
    @PutMapping("/{id}/sections/{sectionId}")
    public ApiResponse<ResumeDetailVO> updateSection(
            @PathVariable String id,
            @PathVariable String sectionId,
            @RequestBody Map<String, Object> content) {
        return ApiResponse.success(resumeHandler.updateResumeSection(id, sectionId, content));
    }

    @Operation(summary = "新增简历模块")
    @PostMapping("/{id}/sections")
    public ApiResponse<ResumeDetailVO> addSection(
            @PathVariable String id,
            @Valid @RequestBody AddSectionRequest request) {
        return ApiResponse.success(resumeHandler.addResumeSection(id, request.getType(), request.getTitle(), request.getContent()));
    }

    @Operation(summary = "删除简历模块")
    @DeleteMapping("/{id}/sections/{sectionId}")
    public ApiResponse<ResumeDetailVO> deleteSection(
            @PathVariable String id,
            @PathVariable String sectionId) {
        return ApiResponse.success(resumeHandler.deleteResumeSection(id, sectionId));
    }

}
