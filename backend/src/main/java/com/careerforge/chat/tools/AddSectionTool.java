package com.careerforge.chat.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.careerforge.chat.dto.tool.SectionSuggestionResponse;
import com.careerforge.common.enums.SectionType;
import com.careerforge.resume.dto.ResumeDetailVO;
import com.careerforge.resume.handler.ResumeHandler;
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
            ResumeDetailVO resume = resumeHandler.getResumeDetail(request.resumeId());
            if (resume == null) {
                return ToolUtils.errorResponse("简历不存在", request.resumeId());
            }

            // 验证区块类型
            String validType = validateSectionType(request.sectionType());
            if (validType == null) {
                return ToolUtils.errorResponse("无效的区块类型: " + request.sectionType(), request.resumeId());
            }

            // 返回新增建议（不直接添加）
            SectionSuggestionResponse response = SectionSuggestionResponse.forAdd(
                request.resumeId(),
                validType,
                request.title(),
                request.content()
            );
            return ToolUtils.toJson(response);
        } catch (Exception e) {
            log.error("[AddSectionTool] 生成新增建议失败", e);
            return ToolUtils.errorResponse(e.getMessage(), request.resumeId());
        }
    }

    private String validateSectionType(String type) {
        SectionType sectionType = SectionType.fromCodeIgnoreCase(type);
        if (sectionType != null && sectionType.isAddable()) {
            return sectionType.getCode();
        }
        return null;
    }

    public static ToolCallback createCallback(ResumeHandler resumeHandler) {
        return FunctionToolCallback.builder("add_section", new AddSectionTool(resumeHandler))
            .description("新增简历区块。此工具返回新增建议，需要用户确认后才会实际执行添加。用于添加新的工作经历、项目经历、教育经历等区块。")
            .inputType(Request.class)
            .build();
    }
}
