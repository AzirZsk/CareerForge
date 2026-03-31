package com.landit.interview.dto.interviewcenter;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新面试轮次状态请求 DTO
 *
 * @author Azir
 */
@Data
public class UpdateRoundStatusRequest {

    @NotBlank(message = "状态不能为空")
    private String status;

}
