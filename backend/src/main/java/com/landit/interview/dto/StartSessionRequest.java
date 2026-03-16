package com.landit.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 开始面试会话请求DTO
 *
 * @author Azir
 */
@Data
public class StartSessionRequest {

    @NotNull(message = "面试类型不能为空")
    private String type;

    @NotBlank(message = "目标岗位不能为空")
    private String position;

    private String difficulty;

    private Integer questionCount;

}
