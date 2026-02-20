package com.landit.resume.entity;

import com.landit.common.entity.BaseEntity;
import com.landit.common.enums.ResumeStatus;
import com.landit.common.enums.ResumeType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 简历实体类
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Resume extends BaseEntity {

    private Long userId;

    private String name;

    private String targetPosition;

    private ResumeType resumeType;

    private Long sourceResumeId;

    private Integer version;

    private ResumeStatus status;

    private Integer score;

    private Integer completeness;

    private Boolean isPrimary;

}
