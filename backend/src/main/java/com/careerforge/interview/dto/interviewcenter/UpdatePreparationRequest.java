package com.careerforge.interview.dto.interviewcenter;

import lombok.Data;

import java.util.List;

/**
 * 更新准备事项请求 DTO
 *
 * @author Azir
 */
@Data
public class UpdatePreparationRequest {

    private String title;

    private String content;

    private String description;

    private Boolean completed;

    private Integer sortOrder;

    /**
     * 优先级：required/recommended/optional
     */
    private String priority;

    /**
     * 关联资源列表
     */
    private List<PreparationResource> resources;

}
