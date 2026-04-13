package com.careerforge.chat.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.careerforge.chat.dto.tool.SectionSuggestionResponse;
import com.careerforge.resume.dto.ResumeDetailVO;
import com.careerforge.resume.handler.ResumeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.function.BiFunction;

/**
 * 删除区块工具
 * 返回删除建议，不直接删除数据，需用户确认后通过 applyChanges API 执行
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class DeleteSectionTool implements BiFunction<DeleteSectionTool.Request, ToolContext, String> {

    private final ResumeHandler resumeHandler;

    public record Request(
        @JsonProperty(required = true)
        @JsonPropertyDescription("简历ID")
        String resumeId,

        @JsonProperty(required = true)
        @JsonPropertyDescription("区块ID")
        String sectionId,

        @JsonProperty(required = false)
        @JsonPropertyDescription("删除原因")
        String reason
    ) {}

    @Override
    public String apply(Request request, ToolContext context) {
        log.info("[DeleteSectionTool] 生成删除建议: resumeId={}, sectionId={}",
            request.resumeId(), request.sectionId());

        try {
            // 获取当前区块内容
            ResumeDetailVO resume = resumeHandler.getResumeDetail(request.resumeId());
            if (resume == null) {
                return ToolUtils.errorResponse("简历不存在", request.resumeId());
            }

            // 查找指定区块
            String sectionType = "";
            String sectionTitle = "";
            String beforeContent = "";
            if (resume.getSections() != null) {
                for (ResumeDetailVO.ResumeSectionVO section : resume.getSections()) {
                    if (request.sectionId().equals(section.getId())) {
                        sectionType = section.getType();
                        sectionTitle = section.getTitle();
                        beforeContent = section.getContent();
                        break;
                    }
                }
            }

            if (sectionType.isEmpty()) {
                return ToolUtils.errorResponseWithSection("区块不存在", request.sectionId());
            }

            // 返回删除建议（不直接删除）
            SectionSuggestionResponse response = SectionSuggestionResponse.forDelete(
                request.sectionId(),
                sectionType,
                sectionTitle,
                beforeContent,
                request.reason()
            );
            return ToolUtils.toJson(response);
        } catch (Exception e) {
            log.error("[DeleteSectionTool] 生成删除建议失败", e);
            return ToolUtils.errorResponseWithSection(e.getMessage(), request.sectionId());
        }
    }

    public static ToolCallback createCallback(ResumeHandler resumeHandler) {
        return FunctionToolCallback.builder("delete_section", new DeleteSectionTool(resumeHandler))
            .description("删除简历区块。此工具返回删除建议，需要用户确认后才会实际执行删除。谨慎使用，建议仅在区块内容完全无关或冗余时使用。")
            .inputType(Request.class)
            .build();
    }
}
