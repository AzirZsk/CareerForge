package com.landit.chat.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.landit.resume.handler.ResumeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.function.BiFunction;

/**
 * 新增区块工具
 * 返回新增建议，不直接添加数据，需用户确认后通过 applyChanges API 执行
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class AddSectionTool implements BiFunction<AddSectionTool.Request, ToolContext, String> {

    private final ResumeHandler resumeHandler;

    public record Request(
        @JsonProperty(required = true)
        @JsonPropertyDescription("简历ID")
        String resumeId,

        @JsonProperty(required = true)
        @JsonPropertyDescription("区块类型：EDUCATION, WORK, PROJECT, SKILLS, CERTIFICATE, OPEN_SOURCE, CUSTOM")
        String sectionType,

        @JsonProperty(required = true)
        @JsonPropertyDescription("区块标题")
        String title,

        @JsonProperty(required = true)
        @JsonPropertyDescription("区块内容（JSON格式字符串）")
        String content
    ) {}

    @Override
    public String apply(Request request, ToolContext context) {
        log.info("[AddSectionTool] 生成新增建议: resumeId={}, type={}, title={}",
            request.resumeId(), request.sectionType(), request.title());

        try {
            // 验证简历存在
            var resume = resumeHandler.getResumeDetail(request.resumeId());
            if (resume == null) {
                return errorResponse("简历不存在", request.resumeId());
            }

            // 验证区块类型
            String validType = validateSectionType(request.sectionType());
            if (validType == null) {
                return errorResponse("无效的区块类型: " + request.sectionType(), request.resumeId());
            }

            // 返回新增建议（不直接添加）
            return buildSuggestionResponse(
                request.resumeId(),
                validType,
                request.title(),
                request.content()
            );
        } catch (Exception e) {
            log.error("[AddSectionTool] 生成新增建议失败", e);
            return errorResponse(e.getMessage(), request.resumeId());
        }
    }

    private String buildSuggestionResponse(
        String resumeId,
        String sectionType,
        String sectionTitle,
        String content
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"action\": \"add\", ");
        sb.append("\"resumeId\": \"").append(resumeId).append("\", ");
        sb.append("\"sectionType\": \"").append(sectionType).append("\", ");
        sb.append("\"sectionTitle\": \"").append(escape(sectionTitle)).append("\", ");
        sb.append("\"afterContent\": \"").append(escape(content)).append("\", ");
        sb.append("\"description\": \"新增区块: ").append(escape(sectionTitle)).append("\"");
        sb.append("}");

        return sb.toString();
    }

    private String errorResponse(String message, String resumeId) {
        return "{\"success\": false, \"error\": \"" + escape(message) + "\", \"resumeId\": \"" + resumeId + "\"}";
    }

    private String validateSectionType(String type) {
        // 允许的区块类型
        String[] validTypes = {"EDUCATION", "WORK", "PROJECT", "SKILLS", "CERTIFICATE", "OPEN_SOURCE", "CUSTOM"};
        for (String valid : validTypes) {
            if (valid.equalsIgnoreCase(type)) {
                return valid;
            }
        }
        return null;
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
        return FunctionToolCallback.builder("add_section", new AddSectionTool(resumeHandler))
            .description("新增简历区块。此工具返回新增建议，需要用户确认后才会实际执行添加。用于添加新的工作经历、项目经历、教育经历等区块。")
            .inputType(Request.class)
            .build();
    }
}
