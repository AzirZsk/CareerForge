package com.landit.interview.dto.interviewcenter;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 添加准备事项请求 DTO
 *
 * @author Azir
 */
@Data
public class AddPreparationRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    private String content;

    /**
     * 优先级：required/recommended/optional，默认 recommended
     */
    private String priority;

    /**
     * 关联资源列表
     */
    private List<PreparationResource> resources;

}
