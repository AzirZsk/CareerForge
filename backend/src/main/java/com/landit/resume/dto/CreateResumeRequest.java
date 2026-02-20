package com.landit.resume.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建简历请求DTO
 *
 * @author Azir
 */
@Data
public class CreateResumeRequest {

    @NotBlank(message = "简历名称不能为空")
    private String name;

    private String targetPosition;

}
