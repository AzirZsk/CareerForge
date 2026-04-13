package com.careerforge.resume.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新增简历模块请求DTO
 *
 * @author Azir
 */
@Data
public class AddSectionRequest {

    @NotBlank(message = "模块类型不能为空")
    private String type;

    @NotBlank(message = "模块标题不能为空")
    private String title;

    private String content;

}
