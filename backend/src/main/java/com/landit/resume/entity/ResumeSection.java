package com.landit.resume.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.landit.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

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

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> content;

    private Integer score;

}
