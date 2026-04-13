package com.careerforge.chat.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.careerforge.chat.dto.tool.GetResumeResponse;
import com.careerforge.resume.dto.ResumeDetailVO;
import com.careerforge.resume.handler.ResumeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.function.BiFunction;

/**
 * 获取简历完整信息工具
 * 返回简历基本信息和所有区块内容
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class GetResumeTool implements BiFunction<GetResumeTool.Request, ToolContext, String> {

    private final ResumeHandler resumeHandler;

    public record Request(
        @JsonProperty(required = true)
        @JsonPropertyDescription("简历ID")
        String resumeId
    ) {}

    @Override
    public String apply(Request request, ToolContext context) {
        log.info("[GetResumeTool] 获取简历信息: resumeId={}", request.resumeId());

        try {
            ResumeDetailVO resume = resumeHandler.getResumeDetail(request.resumeId());
            if (resume == null) {
                return ToolUtils.errorResponse("简历不存在", request.resumeId());
            }

            GetResumeResponse response = GetResumeResponse.from(resume);
            return ToolUtils.toJson(response);
        } catch (Exception e) {
            log.error("[GetResumeTool] 获取简历失败", e);
            return ToolUtils.errorResponse(e.getMessage(), request.resumeId());
        }
    }

    public static ToolCallback createCallback(ResumeHandler resumeHandler) {
        return FunctionToolCallback.builder("get_resume", new GetResumeTool(resumeHandler))
            .description("获取简历完整信息，包含基本信息和所有区块内容。当需要了解简历整体情况时使用此工具。")
            .inputType(Request.class)
            .build();
    }
}
