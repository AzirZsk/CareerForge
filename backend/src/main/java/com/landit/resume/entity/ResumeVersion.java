package com.landit.resume.entity;

import com.landit.common.entity.BaseEntity;
import com.landit.common.enums.ChangeType;
import com.landit.common.enums.ResumeStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 简历历史版本实体类
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ResumeVersion extends BaseEntity {

    private Long resumeId;

    private Integer version;

    private String name;

    private String targetPosition;

    private ResumeStatus status;

    private Integer score;

    private Integer completeness;

    private String changeSummary;

    private ChangeType changeType;

}
