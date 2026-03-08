package com.landit.resume.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.landit.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 简历模块实体类
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_resume_section", autoResultMap = true)
public class ResumeSection extends BaseEntity {

    private String resumeId;

    private String resumeVersionId;

    private String type;

    private String title;

    /**
     * 模块内容，存储为JSON字符串
     */
    private String content;

    private Integer score;

}
