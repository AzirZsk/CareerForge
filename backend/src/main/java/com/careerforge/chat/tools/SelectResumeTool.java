package com.careerforge.chat.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.careerforge.chat.dto.tool.SelectResumeResponse;
import com.careerforge.resume.dto.ResumeDetailVO;
import com.careerforge.resume.handler.ResumeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.function.BiFunction;

/**
 * 选择简历工具
 * AI根据用户意图选择一份简历作为当前对话的上下文
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class SelectResumeTool implements BiFunction<SelectResumeTool.Request, ToolContext, String> {

    private final ResumeHandler resumeHandler;

    public record Request(
            @JsonProperty(required = true)
            @JsonPropertyDescription("要选择的简历ID")
            String resumeId
    ) {}

    @Override
    public String apply(Request request, ToolContext context) {
        log.info("[SelectResumeTool] 选择简历: resumeId={}", request.resumeId());

        try {
            ResumeDetailVO resume = resumeHandler.getResumeDetail(request.resumeId());
            if (resume == null) {
                log.warn("[SelectResumeTool] 简历不存在: resumeId={}", request.resumeId());
                return ToolUtils.toJson(SelectResumeResponse.failure("简历不存在"));
            }
            SelectResumeResponse response = SelectResumeResponse.success(
                    resume.getId(),
                    resume.getName(),
                    resume.getTargetPosition()
            );
            log.info("[SelectResumeTool] 成功选择简历: name={}, targetPosition={}",
                    resume.getName(), resume.getTargetPosition());
            return ToolUtils.toJson(response);
        } catch (Exception e) {
            log.error("[SelectResumeTool] 选择简历失败", e);
            return ToolUtils.toJson(SelectResumeResponse.failure(e.getMessage()));
        }
    }

    public static ToolCallback createCallback(ResumeHandler resumeHandler) {
        return FunctionToolCallback.builder("select_resume", new SelectResumeTool(resumeHandler))
                .description("选择一份简历作为当前对话的上下文。当用户提到简历相关操作（如优化、修改、查看等）且 get_resume_list 返回的列表中能匹配到具体简历时，调用此工具选择对应的简历ID。选择后，后续对话将基于该简历进行。")
                .inputType(Request.class)
                .build();
    }
}
