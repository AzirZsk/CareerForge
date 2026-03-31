package com.landit.interview.dto.interviewcenter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 创建真实面试请求 DTO
 *
 * @author Azir
 */
@Data
public class CreateInterviewRequest {

    @NotBlank(message = "公司名称不能为空")
    private String companyName;

    @NotBlank(message = "目标岗位不能为空")
    private String position;

    @NotNull(message = "面试日期不能为空")
    private LocalDate interviewDate;

    private String jdContent;

    @Valid
    private List<AddRoundRequest> rounds;

    private String notes;

}
