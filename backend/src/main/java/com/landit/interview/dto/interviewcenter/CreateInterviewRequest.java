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

    /**
     * 关联简历ID（用于面试准备参考，指定简历可获得更精准的面试准备建议）
     */
    private String resumeId;

    /**
     * 公司名称（当 jobPositionId 为空时必填）
     */
    private String companyName;

    /**
     * 职位名称（当 jobPositionId 为空时必填）
     */
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

    private String interviewType;

    private String location;

    private String onlineLink;

    private String meetingPassword;

    private String jdContent;

    private String notes;

}
