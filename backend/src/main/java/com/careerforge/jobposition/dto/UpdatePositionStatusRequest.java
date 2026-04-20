package com.careerforge.jobposition.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新职位状态请求
 *
 * @author Azir
 */
@Data
public class UpdatePositionStatusRequest {

    /**
     * 职位状态（draft/applied/interviewing/offered/rejected/withdrawn）
     */
    @NotBlank(message = "状态不能为空")
    private String status;

}
