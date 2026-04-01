package com.landit.interview.dto.interviewcenter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建真实面试请求 DTO
 *
 * @author Azir
 */
@Data
public class CreateInterviewRequest {

    /**
     * 关联职位ID（可选，如果提供则自动关联职位）
     */
    private String jobPositionId;

    @NotBlank(message = "公司名称不能为空")
    private String companyName;

    @NotBlank(message = "目标岗位不能为空")
    private String position;

    @NotNull(message = "面试时间不能为空")
    private LocalDateTime interviewDate;

    @NotBlank(message = "轮次类型不能为空")
    private String roundType;

    private String roundName;

    private String jdContent;

    @Valid
    private List<AddRoundRequest> rounds;

    private String notes;

}
