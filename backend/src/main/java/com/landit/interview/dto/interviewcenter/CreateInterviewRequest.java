package com.landit.interview.dto.interviewcenter;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建面试请求
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

    @NotBlank(message = "职位名称不能为空")
    private String position;

    private LocalDateTime interviewDate;

    /**
     * 轮次类型（面试属性）
     */
    private String roundType;

    /**
     * 自定义轮次名称
     */
    private String roundName;

    private String jdContent;

    private String notes;

}
