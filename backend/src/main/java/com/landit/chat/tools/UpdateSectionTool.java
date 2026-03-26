package com.landit.chat.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.landit.chat.dto.tool.SectionSuggestionResponse;
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
                return ToolUtils.errorResponse("简历不存在", request.resumeId());
            }

            // 查找指定区块获取原始内容
            String beforeContent = "";
            String sectionType = "";
            String sectionTitle = "";
            if (resume.getSections() != null) {
                for (ResumeDetailVO.ResumeSectionVO section : resume.getSections()) {
                    if (request.sectionId().equals(section.getId())) {
                        beforeContent = section.getContent();
                        sectionType = section.getType();
                        sectionTitle = section.getTitle();
                        break;
                    }
                }
            }

            if (sectionType.isEmpty()) {
                return ToolUtils.errorResponseWithSection("区块不存在", request.sectionId());
            }

            // 返回修改建议（不直接修改）
            SectionSuggestionResponse response = SectionSuggestionResponse.forUpdate(
                request.sectionId(),
                sectionType,
                sectionTitle,
                beforeContent,
                request.content(),
                request.reason()
            );
            return ToolUtils.toJson(response);
        } catch (Exception e) {
            log.error("[UpdateSectionTool] 生成更新建议失败", e);
            return ToolUtils.errorResponseWithSection(e.getMessage(), request.sectionId());
        }
    }

    public static ToolCallback createCallback(ResumeHandler resumeHandler) {
        return FunctionToolCallback.builder("update_section", new UpdateSectionTool(resumeHandler))
            .description("更新简历区块内容。此工具返回修改建议，需要用户确认后才会实际执行修改。用于优化或修改现有区块内容。")
            .inputType(Request.class)
            .build();
    }
}
