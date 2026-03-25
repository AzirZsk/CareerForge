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
 * 更新区块内容工具
 * 返回修改建议，不直接修改数据，需用户确认后通过 applyChanges API 执行
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class UpdateSectionTool implements BiFunction<UpdateSectionTool.Request, ToolContext, String> {

    private final ResumeHandler resumeHandler;

    public record Request(
        @JsonProperty(required = true)
        @JsonPropertyDescription("简历ID")
        String resumeId,

        @JsonProperty(required = true)
        @JsonPropertyDescription("区块ID")
        String sectionId,

        @JsonProperty(required = true)
        @JsonPropertyDescription("新的区块内容（JSON格式字符串）")
        String content,

        @JsonProperty(required = false)
        @JsonPropertyDescription("修改原因或说明")
        String reason
    ) {}

    @Override
    public String apply(Request request, ToolContext context) {
        log.info("[UpdateSectionTool] 生成更新建议: resumeId={}, sectionId={}", request.resumeId(), request.sectionId());

        try {
            // 获取当前区块内容
            ResumeDetailVO resume = resumeHandler.getResumeDetail(request.resumeId());
            if (resume == null) {
                return errorResponse("简历不存在", request.resumeId());
            }

            // 查找指定区块获取原始内容
            String beforeContent = "";
            String sectionType = "";
            String sectionTitle = "";
            if (resume.getSections() != null) {
                for (var section : resume.getSections()) {
                    if (request.sectionId().equals(section.getId())) {
                        beforeContent = section.getContent();
                        sectionType = section.getType();
                        sectionTitle = section.getTitle();
                        break;
                    }
                }
            }

            if (sectionType.isEmpty()) {
                return errorResponse("区块不存在", request.sectionId());
            }

            // 返回修改建议（不直接修改）
            return buildSuggestionResponse(
                request.sectionId(),
                sectionType,
                sectionTitle,
                beforeContent,
                request.content(),
                request.reason()
            );
        } catch (Exception e) {
            log.error("[UpdateSectionTool] 生成更新建议失败", e);
            return errorResponse(e.getMessage(), request.sectionId());
        }
    }

    private String buildSuggestionResponse(
        String sectionId,
        String sectionType,
        String sectionTitle,
        String beforeContent,
        String afterContent,
        String reason
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"action\": \"update\", ");
        sb.append("\"sectionId\": \"").append(sectionId).append("\", ");
        sb.append("\"sectionType\": \"").append(sectionType).append("\", ");
        sb.append("\"sectionTitle\": \"").append(escape(sectionTitle)).append("\", ");
        sb.append("\"beforeContent\": \"").append(escape(beforeContent)).append("\", ");
        sb.append("\"afterContent\": \"").append(escape(afterContent)).append("\", ");
        sb.append("\"description\": \"").append(escape(reason != null ? reason : "更新区块内容")).append("\"");
        sb.append("}");

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
        return FunctionToolCallback.builder("update_section", new UpdateSectionTool(resumeHandler))
            .description("更新简历区块内容。此工具返回修改建议，需要用户确认后才会实际执行修改。用于优化或修改现有区块内容。")
            .inputType(Request.class)
            .build();
    }
}
