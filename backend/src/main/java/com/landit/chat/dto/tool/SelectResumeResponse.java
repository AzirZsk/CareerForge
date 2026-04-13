package com.landit.chat.dto.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 选择简历响应类
 * AI调用 select_resume 工具后的响应
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectResumeResponse extends ToolResponse {

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
