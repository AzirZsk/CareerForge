package com.landit.chat.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.handler.ResumeHandler;
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
                return errorResponse("简历不存在", request.resumeId());
            }

            return buildSuccessResponse(resume);
        } catch (Exception e) {
            log.error("[GetResumeTool] 获取简历失败", e);
            return errorResponse(e.getMessage(), request.resumeId());
        }
    }

    private String buildSuccessResponse(ResumeDetailVO resume) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\": true, \"data\": {");
        sb.append("\"name\": \"").append(escape(resume.getName())).append("\", ");
        sb.append("\"targetPosition\": \"").append(escape(resume.getTargetPosition())).append("\", ");

        if (resume.getOverallScore() != null) {
            sb.append("\"overallScore\": ").append(resume.getOverallScore()).append(", ");
        }

        sb.append("\"sections\": [");
        if (resume.getSections() != null) {
            for (int i = 0; i < resume.getSections().size(); i++) {
                var section = resume.getSections().get(i);
                if (i > 0) sb.append(", ");
                sb.append("{");
                sb.append("\"id\": \"").append(section.getId()).append("\", ");
                sb.append("\"type\": \"").append(section.getType()).append("\", ");
                sb.append("\"title\": \"").append(escape(section.getTitle())).append("\", ");
                sb.append("\"content\": \"").append(escape(section.getContent())).append("\"");
                if (section.getScore() != null) {
                    sb.append(", \"score\": ").append(section.getScore());
                }
                sb.append("}");
            }
        }
        sb.append("]}}");

        return sb.toString();
    }

    private String errorResponse(String message, String resumeId) {
        return "{\"success\": false, \"error\": \"" + escape(message) + "\", \"resumeId\": \"" + resumeId + "\"}";
    }

    private String escape(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    public static ToolCallback createCallback(ResumeHandler resumeHandler) {
        return FunctionToolCallback.builder("get_resume", new GetResumeTool(resumeHandler))
            .description("获取简历完整信息，包含基本信息和所有区块内容。当需要了解简历整体情况时使用此工具。")
            .inputType(Request.class)
            .build();
    }
}
