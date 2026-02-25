package com.landit.resume.controller;

import com.landit.common.enums.ResumeStatus;
import com.landit.common.response.ApiResponse;
import com.landit.resume.dto.AddSectionRequest;
import com.landit.resume.dto.CreateResumeRequest;
import com.landit.resume.dto.DeriveResumeRequest;
import com.landit.resume.dto.DiagnoseResumeRequest;
import com.landit.resume.dto.DiagnoseResumeResponse;
import com.landit.resume.dto.MatchJobRequest;
import com.landit.resume.dto.MatchJobResponse;
import com.landit.resume.dto.OptimizeResumeRequest;
import com.landit.resume.dto.OptimizeResumeResponse;
import com.landit.resume.dto.OptimizeSectionRequest;
import com.landit.resume.dto.OptimizeSectionResponse;
import com.landit.resume.dto.PrimaryResumeVO;
import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.dto.ResumeSuggestionVO;
import com.landit.resume.dto.ResumeVersionVO;
import com.landit.resume.dto.UpdateResumeRequest;
import com.landit.resume.entity.Resume;
import com.landit.resume.handler.ResumeHandler;
import com.landit.resume.service.ResumeService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
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

    private final ResumeService resumeService;
    private final ResumeHandler resumeHandler;

    @Operation(summary = "获取简历列表")
    @GetMapping
    public ApiResponse<List<Resume>> getResumes(@RequestParam(required = false) ResumeStatus status) {
        return ApiResponse.success(resumeService.getResumes(status));
    }

    @Operation(summary = "获取主简历")
    @GetMapping("/primary")
    public ApiResponse<PrimaryResumeVO> getPrimaryResume() {
        return ApiResponse.success(resumeHandler.getPrimaryResume());
    }

    @Operation(summary = "解析简历文件为图片列表")
    @PostMapping("/parse")
    public ApiResponse<List<String>> parseResume(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(resumeHandler.parseResumeToImages(file));
    }

    @Operation(summary = "获取简历详情")
    @GetMapping("/{id}")
    public ApiResponse<ResumeDetailVO> getResumeDetail(@PathVariable String id) {
        return ApiResponse.success(resumeHandler.getResumeDetail(id));
    }

    @Operation(summary = "上传简历文件")
    @PostMapping("/upload")
    public ApiResponse<ResumeDetailVO> uploadResume(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(resumeHandler.uploadResume(file));
    }

    @Operation(summary = "创建空白简历")
    @PostMapping
    public ApiResponse<Resume> createResume(@Valid @RequestBody CreateResumeRequest request) {
        return ApiResponse.success(resumeService.createResume(request));
    }

    @Operation(summary = "更新简历")
    @PutMapping("/{id}")
    public ApiResponse<ResumeDetailVO> updateResume(@PathVariable String id, @RequestBody UpdateResumeRequest request) {
        return ApiResponse.success(resumeService.updateResume(id, request));
    }

    @Operation(summary = "删除简历")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteResume(@PathVariable String id) {
        resumeService.deleteResume(id);
        return ApiResponse.success();
    }

    @Operation(summary = "设置主简历")
    @PutMapping("/{id}/primary")
    public ApiResponse<Void> setPrimaryResume(@PathVariable String id) {
        resumeService.setPrimaryResume(id);
        return ApiResponse.success();
    }

    // ==================== 简历优化相关API ====================

    @Operation(summary = "简历诊断", description = "分析简历质量，给出评分和改进建议。preciseMode=true时联网搜索最新岗位要求")
    @PostMapping("/{id}/diagnose")
    public ApiResponse<DiagnoseResumeResponse> diagnoseResume(
            @PathVariable String id,
            @Valid @RequestBody DiagnoseResumeRequest request) {
        return ApiResponse.success(resumeHandler.diagnoseResume(id, request));
    }

    @Operation(summary = "优化简历模块", description = "AI优化简历的指定模块内容")
    @PostMapping("/{id}/optimize-section")
    public ApiResponse<OptimizeSectionResponse> optimizeSection(
            @PathVariable String id,
            @Valid @RequestBody OptimizeSectionRequest request) {
        return ApiResponse.success(resumeHandler.optimizeSection(id, request));
    }

    @Operation(summary = "岗位JD匹配", description = "分析简历与具体岗位JD的匹配程度")
    @PostMapping("/{id}/match-job")
    public ApiResponse<MatchJobResponse> matchJob(
            @PathVariable String id,
            @Valid @RequestBody MatchJobRequest request) {
        return ApiResponse.success(resumeHandler.matchJob(id, request));
    }

    @Operation(summary = "获取优化建议")
    @GetMapping("/{id}/suggestions")
    public ApiResponse<List<ResumeSuggestionVO>> getResumeSuggestions(@PathVariable String id) {
        return ApiResponse.success(resumeService.getResumeSuggestions(id));
    }

    @Operation(summary = "应用优化建议")
    @PostMapping("/{id}/suggestions/{suggestionId}/apply")
    public ApiResponse<ResumeDetailVO> applyResumeSuggestion(@PathVariable String id, @PathVariable String suggestionId) {
        return ApiResponse.success(resumeHandler.applyResumeSuggestion(id, suggestionId));
    }

    @Operation(summary = "AI优化简历")
    @PostMapping("/{id}/optimize")
    public ApiResponse<OptimizeResumeResponse> optimizeResume(@PathVariable String id, @RequestBody OptimizeResumeRequest request) {
        return ApiResponse.success(resumeHandler.optimizeResume(id, request));
    }

    @Operation(summary = "导出简历PDF")
    @GetMapping("/{id}/export")
    public byte[] exportResume(@PathVariable String id) {
        return resumeHandler.exportResume(id);
    }

    @Operation(summary = "获取简历版本历史")
    @GetMapping("/{id}/versions")
    public ApiResponse<List<ResumeVersionVO>> getVersionHistory(@PathVariable String id) {
        return ApiResponse.success(resumeService.getVersionHistory(id));
    }

    @Operation(summary = "获取指定版本详情")
    @GetMapping("/{id}/versions/{version}")
    public ApiResponse<ResumeDetailVO> getVersionDetail(@PathVariable String id, @PathVariable Integer version) {
        return ApiResponse.success(resumeService.getVersionDetail(id, version));
    }

    @Operation(summary = "回滚到指定版本")
    @PostMapping("/{id}/rollback/{version}")
    public ApiResponse<ResumeDetailVO> rollbackToVersion(@PathVariable String id, @PathVariable Integer version) {
        return ApiResponse.success(resumeHandler.rollbackToVersion(id, version));
    }

    @Operation(summary = "基于主简历派生岗位定制简历")
    @PostMapping("/{id}/derive")
    public ApiResponse<Resume> deriveResume(@PathVariable String id, @Valid @RequestBody DeriveResumeRequest request) {
        return ApiResponse.success(resumeHandler.deriveResume(id, request));
    }

    // ==================== 模块级操作 ====================

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
