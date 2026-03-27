package com.landit.chat.dto.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 选择简历响应类
 * AI调用 select_resume 工具后的响应
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectResumeResponse extends ToolResponse {

    private static final String RESPONSE_TYPE = "resume_selected";

    /**
     * 响应类型，固定为 "resume_selected"
     * 用于 AIChatService 识别这是简历选择响应
     */
    @JsonProperty("responseType")
    private String responseType = RESPONSE_TYPE;

    /**
     * 选中的简历ID
     */
    private String resumeId;

    /**
     * 选中的简历名称
     */
    private String resumeName;

    /**
     * 选中的简历目标岗位
     */
    private String targetPosition;

    /**
     * 创建成功响应
     */
    public static SelectResumeResponse success(String resumeId, String resumeName, String targetPosition) {
        SelectResumeResponse response = new SelectResumeResponse();
        response.setSuccess(true);
        response.setResumeId(resumeId);
        response.setResumeName(resumeName);
        response.setTargetPosition(targetPosition);
        return response;
    }

    /**
     * 创建失败响应
     */
    public static SelectResumeResponse failure(String errorMessage) {
        SelectResumeResponse response = new SelectResumeResponse();
        response.setSuccess(false);
        response.setError(errorMessage);
        return response;
    }
}
