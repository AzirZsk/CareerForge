package com.landit.interview.dto;

import com.landit.common.enums.InterviewType;
import com.landit.common.enums.QuestionDifficulty;
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
    private InterviewType type;

    @NotBlank(message = "目标岗位不能为空")
    private String position;

    private QuestionDifficulty difficulty;

    private Integer questionCount;

}
