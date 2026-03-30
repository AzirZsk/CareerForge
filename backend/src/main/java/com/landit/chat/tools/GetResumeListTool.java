package com.landit.chat.tools;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.landit.chat.dto.tool.ResumeBriefVO;
import com.landit.resume.dto.ResumeListVO;
import com.landit.resume.handler.ResumeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import com.landit.chat.dto.tool.ToolResponse;
import java.util.List;
import java.util.function.BiFunction;

/**
 * 获取简历列表工具
 * 返回用户所有简历的简要信息，供AI判断用户想操作哪份简历
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class GetResumeListTool implements BiFunction<GetResumeListTool.Request, ToolContext, String> {

    private final ResumeHandler resumeHandler;

    public record Request(
            @JsonPropertyDescription("用户消息内容，用于筛选相关简历（可选）")
            String userMessage
    ) {}

    @Override
    public String apply(Request request, ToolContext context) {
        log.info("[GetResumeListTool] 获取简历列表");

        try {
            List<ResumeListVO> resumes = resumeHandler.getAllResumes(null);
            if (resumes == null || resumes.isEmpty()) {
                return ToolUtils.toJson(GetResumeListResponse.empty());
            }
            List<ResumeBriefVO> briefList = resumes.stream()
                    .map(ResumeBriefVO::from)
                    .toList();
            GetResumeListResponse response = GetResumeListResponse.success(briefList);
            log.info("[GetResumeListTool] 返回 {} 份简历", briefList.size());
            return ToolUtils.toJson(response);
        } catch (Exception e) {
            log.error("[GetResumeListTool] 获取简历列表失败", e);
            return ToolUtils.toJson(GetResumeListResponse.failure(e.getMessage()));
        }
    }

    public static ToolCallback createCallback(ResumeHandler resumeHandler) {
        return FunctionToolCallback.builder("get_resume_list", new GetResumeListTool(resumeHandler))
                .description("获取用户所有简历的简要信息列表。当用户提到简历相关操作（如优化、修改、查看等）但未明确指定是哪份简历时，调用此工具获取简历列表，然后根据用户描述匹配最合适的简历。")
                .inputType(Request.class)
                .build();
    }

    /**
     * 获取简历列表响应
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class GetResumeListResponse extends ToolResponse {
        private static final long serialVersionUID = 1L;
        private List<ResumeBriefVO> resumes;
        private Integer count;

        public static GetResumeListResponse success(List<ResumeBriefVO> resumes) {
            GetResumeListResponse response = new GetResumeListResponse();
            response.setSuccess(true);
            response.setResumes(resumes);
            response.setCount(resumes.size());
            return response;
        }

        public static GetResumeListResponse empty() {
            GetResumeListResponse response = new GetResumeListResponse();
            response.setSuccess(true);
            response.setResumes(List.of());
            response.setCount(0);
            return response;
        }

        public static GetResumeListResponse failure(String errorMessage) {
            GetResumeListResponse response = new GetResumeListResponse();
            response.setSuccess(false);
            response.setError(errorMessage);
            return response;
        }
    }
}
