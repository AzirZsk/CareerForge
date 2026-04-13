package com.landit.task.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务 VO
 *
 * @author Azir
 */
@Data
public class TaskVO {

    /**
     * 任务ID
     */
    private String id;

    /**
     * 任务类型
     */
    private String taskType;

    /**
     * 任务类型标签
     */
    private String taskTypeLabel;

    /**
     * 关联业务ID
     */
    private String businessId;

    /**
     * 任务状态
     */
    private String status;

    /**
     * 任务状态标签
     */
    private String statusLabel;

    /**
     * 进度 0-100
     */
    private Integer progress;

    /**
     * 状态消息
     */
    private String message;

    /**
     * 任务结果（JSON格式）
     */
    private String result;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

}
