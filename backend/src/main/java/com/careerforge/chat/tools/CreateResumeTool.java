package com.careerforge.chat.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.careerforge.chat.dto.tool.CreateResumeResponse;
import com.careerforge.resume.dto.CreateResumeRequest;
import com.careerforge.resume.dto.ResumeDetailVO;
import com.careerforge.resume.handler.ResumeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.function.BiFunction;

/**
 * 创建简历工具
 * 通过对话创建新的空白简历
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class CreateResumeTool implements BiFunction<CreateResumeTool.Request, ToolContext, String> {

    private final ResumeHandler resumeHandler;

    public record Request(
        @JsonProperty(required = true)
        @JsonPropertyDescription("简历名称")
        String name,

        @JsonProperty(required = false)
        @JsonPropertyDescription("目标岗位（可选）")
        String targetPosition
    ) {}

    @Override
    public String apply(Request request, ToolContext context) {
        log.info("[CreateResumeTool] 创建简历: name={}, targetPosition={}",
            request.name(), request.targetPosition());

        try {
            // 参数校验
            if (request.name() == null || request.name().isBlank()) {
                return ToolUtils.errorResponse("简历名称不能为空");
            }

            // 创建空白简历
            String resumeName = request.name().trim();
            String targetPosition = request.targetPosition() != null ? request.targetPosition().trim() : null;

            ResumeDetailVO resume = resumeHandler.createBlankResume(
                new CreateResumeRequest(resumeName, targetPosition)
            );

            CreateResumeResponse response = CreateResumeResponse.from(resume);
            return ToolUtils.toJson(response);
        } catch (Exception e) {
            log.error("[CreateResumeTool] 创建简历失败", e);
            return ToolUtils.errorResponse(e.getMessage());
        }
    }

    public static ToolCallback createCallback(ResumeHandler resumeHandler) {
        return FunctionToolCallback.builder("create_resume", new CreateResumeTool(resumeHandler))
            .description("创建新的空白简历。当用户需要创建简历时使用此工具。创建成功后，用户可以选择这份简历进行进一步操作。")
            .inputType(Request.class)
            .build();
    }
}
