package com.landit.resume.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新简历基本信息请求DTO
 * 用于更新简历名称和目标岗位
 *
 * @author Azir
 */
@Data
public class UpdateResumeRequest {

    @NotBlank(message = "简历名称不能为空")
    private String name;

    private String targetPosition;

}
