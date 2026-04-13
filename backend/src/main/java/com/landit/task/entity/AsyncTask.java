package com.landit.task.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.landit.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 异步任务实体类
 * 支持音频转录、简历优化、复盘分析等异步任务
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_async_task")
public class AsyncTask extends BaseEntity {

    /**
     * 所属用户ID
     */
    private String userId;

    /**
     * 任务类型
     * audio_transcribe - 音频转录
     * resume_optimize - 简历优化
     * review_analysis - 复盘分析
     */
    private String taskType;

    /**
     * 关联业务ID（面试ID/简历ID）
     */
    private String businessId;

    /**
     * 任务状态
     * pending - 等待中
     * running - 执行中
     * completed - 已完成
     * failed - 失败
     */
    private String status;

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

}
