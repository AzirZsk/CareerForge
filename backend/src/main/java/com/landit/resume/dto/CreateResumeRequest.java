package com.landit.resume.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建空白简历请求DTO
 * 所有字段均为可选，默认创建名为"新简历"的空白简历
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateResumeRequest {

    /**
     * 简历名称（可选，默认"新简历"）
     */
    @Size(max = 100, message = "简历名称不能超过100个字符")
    private String name;

    /**
     * 目标岗位（可选）
     */
    @Size(max = 100, message = "目标岗位不能超过100个字符")
    private String targetPosition;

}
