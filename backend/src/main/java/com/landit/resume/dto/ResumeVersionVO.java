package com.landit.resume.dto;

import com.landit.common.enums.ChangeType;
import com.landit.common.enums.ResumeStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 简历版本VO
 *
 * @author Azir
 */
@Data
public class ResumeVersionVO {

    private Long id;

    private Long resumeId;

    private Integer version;

    private String name;

    private String targetPosition;

    private ResumeStatus status;

    private Integer score;

    private Integer completeness;

    private String changeSummary;

    private ChangeType changeType;

    private LocalDateTime createdAt;

}
