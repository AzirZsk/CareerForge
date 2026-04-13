package com.careerforge.interview.dto.interviewcenter;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 准备事项 VO
 *
 * @author Azir
 */
@Data
public class PreparationVO {

    private String id;

    private String interviewId;

    private String itemType;

    private String title;

    private String content;

    private Boolean completed;

    private String source;

    private Integer sortOrder;

    /**
     * 优先级：required/recommended/optional
     */
    private String priority;

    /**
     * 关联资源列表
     */
    private List<PreparationResource> resources;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
