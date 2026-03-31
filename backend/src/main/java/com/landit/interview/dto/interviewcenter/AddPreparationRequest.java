package com.landit.interview.dto.interviewcenter;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 添加准备事项请求 DTO
 *
 * @author Azir
 */
@Data
public class AddPreparationRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    private String content;

}
