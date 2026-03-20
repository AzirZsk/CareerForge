package com.landit.resume.controller;

import com.landit.common.response.ApiResponse;
import com.landit.resume.dto.ResumeSuggestionVO;
import com.landit.resume.handler.ResumeSuggestionHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 简历优化建议控制器
 *
 * @author Azir
 */
@Tag(name = "简历优化建议", description = "简历优化建议相关接口")
@RestController
@RequestMapping("/suggestions")
@RequiredArgsConstructor
public class ResumeSuggestionController {

    private final ResumeSuggestionHandler suggestionHandler;

    @Operation(summary = "获取简历优化建议列表")
    @GetMapping("/resume/{resumeId}")
    public ApiResponse<List<ResumeSuggestionVO>> getSuggestions(
            @PathVariable String resumeId) {
        return ApiResponse.success(suggestionHandler.getSuggestions(resumeId));
    }

    @Operation(summary = "删除优化建议")
    @DeleteMapping("/{suggestionId}")
    public ApiResponse<Void> deleteSuggestion(
            @PathVariable String suggestionId) {
        suggestionHandler.deleteSuggestion(suggestionId);
        return ApiResponse.success(null);
    }

}
