package com.landit.interview.dto.interviewcenter;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 添加面试轮次请求 DTO
 *
 * @author Azir
 */
@Data
public class AddRoundRequest {

    @NotBlank(message = "轮次类型不能为空")
    private String roundType;

    private String roundName;

    private LocalDateTime scheduledDate;

}
