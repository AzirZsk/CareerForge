package com.landit.interview.dto.interviewcenter;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 面试准备事项 VO
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

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
