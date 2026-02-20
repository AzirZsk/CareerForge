package com.landit.job.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.landit.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 职位实体类
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_job", autoResultMap = true)
public class Job extends BaseEntity {

    private String company;

    private String companyLogo;

    private String position;

    private String salary;

    private String location;

    private String experience;

    private String education;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    private Integer matchScore;

    private LocalDateTime publishedAt;

    private String description;

}
