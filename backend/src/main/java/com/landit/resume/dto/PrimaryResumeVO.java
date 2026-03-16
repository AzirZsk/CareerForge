package com.landit.resume.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 主简历VO
 *
 * @author Azir
 */
@Data
public class PrimaryResumeVO {

    private String id;

    private String name;

    private String targetPosition;

    private String status;

    private Integer score;

    private Integer completeness;

    /**
     * 是否已完成分析
     * true: 已完成AI结构化分析
     * false: 仅有原始文本，未完成分析
     */
    private Boolean analyzed;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
