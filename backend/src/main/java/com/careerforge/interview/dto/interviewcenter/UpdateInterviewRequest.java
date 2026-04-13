package com.careerforge.interview.dto.interviewcenter;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 更新面试基本信息请求 DTO
 *
 * @author Azir
 */
@Data
public class UpdateInterviewRequest {

    private String companyName;

    private String position;

    private LocalDateTime interviewDate;

    private String roundType;

    private String roundName;

    private String interviewType;

    private String location;

    private String onlineLink;

    private String meetingPassword;

    private String jdContent;

    private String notes;

    private String status;

    private String overallResult;

    /**
     * 关联简历ID（用于面试准备参考）
     */
    private String resumeId;

    /**
     * 面试过程转译文本
     */
    private String transcript;

}
