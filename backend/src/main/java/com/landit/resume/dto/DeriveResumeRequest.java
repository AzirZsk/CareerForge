package com.landit.resume.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 派生简历请求DTO
 *
 * @author Azir
 */
@Data
public class DeriveResumeRequest {

    @NotBlank(message = "目标岗位不能为空")
    private String targetPosition;

    private String resumeName;

    /**
     * 职位描述（Job Description），用于定制简历
     */
    @NotBlank(message = "职位描述不能为空")
    private String jobDescription;

}
