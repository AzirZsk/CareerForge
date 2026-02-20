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

}
