package com.landit.resume.controller;

import com.landit.common.response.ApiResponse;
import com.landit.resume.dto.AddSectionRequest;
import com.landit.resume.dto.ApplyOptimizeRequest;
import com.landit.resume.dto.CreateResumeRequest;
import com.landit.resume.dto.DeriveResumeRequest;
import com.landit.resume.dto.PrimaryResumeVO;
import com.landit.resume.dto.ResumeListVO;
import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.dto.UpdateSectionRequest;
import com.landit.resume.entity.Resume;
import com.landit.resume.handler.ResumeHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    // ==================== 简历列表管理 API ====================

    @Operation(summary = "获取所有简历列表")
    @GetMapping
    public ApiResponse<List<ResumeListVO>> getAllResumes(
            @Parameter(description = "简历状态筛选（optimized/draft），不传则返回全部")
            @RequestParam(required = false) String status) {
        return ApiResponse.success(resumeHandler.getAllResumes(status));
    }

    @Operation(summary = "创建空白简历")
    @PostMapping
    public ApiResponse<ResumeDetailVO> createResume(@Valid @RequestBody CreateResumeRequest request) {
        return ApiResponse.success(resumeHandler.createBlankResume(request));
    }

    @Operation(summary = "删除简历")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteResume(@PathVariable String id) {
        resumeHandler.deleteResume(id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "设置主简历")
    @PutMapping("/{id}/primary")
    public ApiResponse<PrimaryResumeVO> setPrimaryResume(@PathVariable String id) {
        return ApiResponse.success(resumeHandler.setPrimaryResume(id));
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
            @Valid @RequestBody UpdateSectionRequest request) {
        return ApiResponse.success(resumeHandler.updateResumeSection(id, sectionId, request));
    }

    @Operation(summary = "新增简历模块")
    @PostMapping("/{id}/sections")
    public ApiResponse<ResumeDetailVO> addSection(
            @PathVariable String id,
            @Valid @RequestBody AddSectionRequest request) {
        return ApiResponse.success(resumeHandler.addResumeSection(id, request));
    }

    @Operation(summary = "删除简历模块")
    @DeleteMapping("/{id}/sections/{sectionId}")
    public ApiResponse<ResumeDetailVO> deleteSection(
            @PathVariable String id,
            @PathVariable String sectionId) {
        return ApiResponse.success(resumeHandler.deleteResumeSection(id, sectionId));
    }

    @Operation(summary = "派生岗位定制简历")
    @PostMapping("/{id}/derive")
    public ApiResponse<Resume> deriveResume(
            @PathVariable String id,
            @Valid @RequestBody DeriveResumeRequest request) {
        return ApiResponse.success(resumeHandler.deriveResume(id, request));
    }

    @Operation(summary = "应用优化变更")
    @PostMapping("/{id}/optimize/apply")
    public ApiResponse<ResumeDetailVO> applyOptimizeChanges(
            @PathVariable String id,
            @Valid @RequestBody ApplyOptimizeRequest request) {
        return ApiResponse.success(resumeHandler.applyOptimizeChanges(id, request));
    }

    @Operation(summary = "删除优化建议")
    @DeleteMapping("/{id}/suggestions/{suggestionId}")
    public ApiResponse<Void> deleteSuggestion(
            @PathVariable String id,
            @PathVariable String suggestionId) {
        resumeHandler.deleteSuggestion(id, suggestionId);
        return ApiResponse.success(null);
    }

}
