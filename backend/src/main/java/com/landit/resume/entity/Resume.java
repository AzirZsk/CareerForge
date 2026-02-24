package com.landit.resume.entity;

import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName(value = "t_resume", autoResultMap = true)
public class Resume extends BaseEntity {

    private String userId;

    private String name;

    private String targetPosition;

    /**
     * 简历原文本（Markdown格式）
     */
    private String markdownContent;

    private ResumeType resumeType;

    private String sourceResumeId;

    private Integer version;

    private ResumeStatus status;

    private Integer score;

    private Integer completeness;

    private Boolean isPrimary;

}
