package com.landit.interview.dto.interviewcenter;

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

}
