package com.landit.resume.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 简历诊断请求DTO
 *
 * @author Azir
 */
@Data
public class DiagnoseResumeRequest {

    /**
     * 目标岗位名称
     */
    @NotBlank(message = "目标岗位不能为空")
    private String targetPosition;

}
