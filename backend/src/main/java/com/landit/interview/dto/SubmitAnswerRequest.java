package com.landit.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提交回答请求DTO
 *
 * @author Azir
 */
@Data
public class SubmitAnswerRequest {

    @NotNull(message = "问题ID不能为空")
    private Long questionId;

    @NotBlank(message = "回答内容不能为空")
    private String answer;

    private LocalDateTime timestamp;

}
