package com.landit.resume.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.landit.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 简历优化建议实体类
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_resume_suggestion", autoResultMap = true)
public class ResumeSuggestion extends BaseEntity {

    private String resumeId;

    /**
     * 关联的简历模块ID
     */
    private String sectionId;

    private String type;

    private String category;

    private String title;

    private String description;

    private String impact;

    private String position;

}
