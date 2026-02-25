package com.landit.resume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 模块内容优化请求DTO
 *
 * @author Azir
 */
@Data
public class OptimizeSectionRequest {

    /**
     * 模块类型：work/project/education/skills/summary
     */
    @NotBlank(message = "模块类型不能为空")
    private String sectionType;

    /**
     * 原始内容
     */
    @NotNull(message = "原始内容不能为空")
    private Map<String, Object> originalContent;

    /**
     * 目标岗位
     */
    @NotBlank(message = "目标岗位不能为空")
    private String targetPosition;

}
