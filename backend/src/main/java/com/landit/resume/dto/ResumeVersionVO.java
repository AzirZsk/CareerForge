package com.landit.resume.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 简历版本VO
 *
 * @author Azir
 */
@Data
public class ResumeVersionVO {

    private String id;

    private String resumeId;

    private Integer version;

    private String name;

    private String targetPosition;

    private String status;

    private Integer score;

    private Integer completeness;

    private String changeSummary;

    private String changeType;

    private LocalDateTime createdAt;

}
