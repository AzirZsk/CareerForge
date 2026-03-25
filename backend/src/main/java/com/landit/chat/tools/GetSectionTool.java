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
 * 获取指定区块内容工具
 * 返回指定区块的详细信息
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class GetSectionTool implements BiFunction<GetSectionTool.Request, ToolContext, String> {

    private final ResumeHandler resumeHandler;

    public record Request(
        @JsonProperty(required = true)
        @JsonPropertyDescription("简历ID")
        String resumeId,

        @JsonProperty(required = true)
        @JsonPropertyDescription("区块ID")
        String sectionId
    ) {}

    @Override
    public String apply(Request request, ToolContext context) {
        log.info("[GetSectionTool] 获取区块内容: resumeId={}, sectionId={}", request.resumeId(), request.sectionId());

        try {
            ResumeDetailVO resume = resumeHandler.getResumeDetail(request.resumeId());
            if (resume == null) {
                return errorResponse("简历不存在", request.resumeId());
            }

            // 查找指定区块
            if (resume.getSections() != null) {
                for (var section : resume.getSections()) {
                    if (request.sectionId().equals(section.getId())) {
                        return buildSuccessResponse(section);
                    }
                }
            }

            return errorResponse("区块不存在", request.sectionId());
        } catch (Exception e) {
            log.error("[GetSectionTool] 获取区块失败", e);
            return errorResponse(e.getMessage(), request.sectionId());
        }
    }

    private String buildSuccessResponse(ResumeDetailVO.ResumeSectionVO section) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\": true, \"data\": {");
        sb.append("\"id\": \"").append(section.getId()).append("\", ");
        sb.append("\"type\": \"").append(section.getType()).append("\", ");
        sb.append("\"title\": \"").append(escape(section.getTitle())).append("\", ");
        sb.append("\"content\": \"").append(escape(section.getContent())).append("\"");
        if (section.getScore() != null) {
            sb.append(", \"score\": ").append(section.getScore());
        }
        sb.append("}}");

        return sb.toString();
    }

    private String errorResponse(String message, String sectionId) {
        return "{\"success\": false, \"error\": \"" + escape(message) + "\", \"sectionId\": \"" + sectionId + "\"}";
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
        return FunctionToolCallback.builder("get_section", new GetSectionTool(resumeHandler))
            .description("获取简历中指定区块的详细内容。当需要查看或修改某个具体区块时使用此工具。")
            .inputType(Request.class)
            .build();
    }
}
