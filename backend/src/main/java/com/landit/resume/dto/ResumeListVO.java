package com.landit.resume.dto;

import com.landit.common.enums.ResumeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 简历列表项VO
 * 用于简历列表展示，包含基本信息和状态
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeListVO {

    /**
     * 简历ID
     */
    private String id;

    /**
     * 简历名称
     */
    private String name;

    /**
     * 目标岗位
     */
    private String targetPosition;

    /**
     * 简历状态
     */
    private ResumeStatus status;

    /**
     * 简历评分（0-100）
     */
    private Integer score;

    /**
     * 完整度（0-100）
     */
    private Integer completeness;

    /**
     * 是否为主简历
     */
    private Boolean isPrimary;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
