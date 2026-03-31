package com.landit.interview.dto.interviewcenter;

import lombok.Data;

import java.time.LocalDate;

/**
 * 更新面试基本信息请求 DTO
 *
 * @author Azir
 */
@Data
public class UpdateInterviewRequest {

    private String companyName;

    private String position;

    private LocalDate interviewDate;

    private String jdContent;

    private String notes;

}
