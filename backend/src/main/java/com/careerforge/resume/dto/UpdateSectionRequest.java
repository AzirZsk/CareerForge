package com.careerforge.resume.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新简历模块请求DTO
 *
 * @author Azir
 */
@Data
public class UpdateSectionRequest {

    /**
     * 模块内容（JSON字符串）
     */
    @NotBlank(message = "模块内容不能为空")
    private String content;

}
