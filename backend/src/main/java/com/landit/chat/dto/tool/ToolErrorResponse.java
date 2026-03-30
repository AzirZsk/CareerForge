package com.landit.chat.dto.tool;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Tool错误响应类
 * 统一的错误响应结构
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ToolErrorResponse extends ToolResponse {

    /**
     * 关联的简历ID（可选）
     */
    private String resumeId;

    /**
     * 关联的区块ID（可选）
     */
    private String sectionId;

    public ToolErrorResponse() {
        this.setSuccess(false);
    }

    public ToolErrorResponse(String error) {
        this.setSuccess(false);
        this.setError(error);
    }

    /**
     * 创建带简历ID的错误响应
     */
    public static ToolErrorResponse of(String error, String resumeId) {
        ToolErrorResponse response = new ToolErrorResponse(error);
        response.setResumeId(resumeId);
        return response;
    }

    /**
     * 创建带区块ID的错误响应
     */
    public static ToolErrorResponse ofSection(String error, String sectionId) {
        ToolErrorResponse response = new ToolErrorResponse(error);
        response.setSectionId(sectionId);
        return response;
    }
}
