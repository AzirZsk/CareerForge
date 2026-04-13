package com.careerforge.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建任务响应
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskResponse {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 任务状态
     */
    private String status;

}
