package com.landit.resume.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 岗位JD匹配请求DTO
 *
 * @author Azir
 */
@Data
public class MatchJobRequest {

    /**
     * 岗位JD内容
     */
    @NotBlank(message = "岗位JD不能为空")
    private String jobDescription;

}
