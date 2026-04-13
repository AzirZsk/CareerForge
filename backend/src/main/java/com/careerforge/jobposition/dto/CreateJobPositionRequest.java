package com.careerforge.jobposition.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建职位请求 DTO
 *
 * @author Azir
 */
@Data
public class CreateJobPositionRequest {

    /**
     * 公司名称
     */
    @NotBlank(message = "公司名称不能为空")
    private String companyName;

    /**
     * 职位名称
     */
    @NotBlank(message = "职位名称不能为空")
    private String title;

    /**
     * JD原文（可选）
     */
    private String jdContent;

}
