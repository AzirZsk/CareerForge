package com.landit.resume.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.landit.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 简历历史版本实体类
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_resume_version", autoResultMap = true)
public class ResumeVersion extends BaseEntity {

    private String resumeId;

    private Integer version;

    private String name;

    private String targetPosition;

    private String status;

    private Integer score;

    private Integer completeness;

    private String changeSummary;

    private String changeType;

}
