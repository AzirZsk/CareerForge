package com.careerforge.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.careerforge.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 面试准备事项实体类
 * 管理面试准备清单
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_interview_preparation")
public class InterviewPreparation extends BaseEntity {

    private String interviewId;

    private String itemType;

    private String title;

    private String content;

    private String description;

    private Boolean completed;

    private String source;

    private Integer sortOrder;

    /**
     * 优先级：required/recommended/optional
     */
    private String priority;

    /**
     * 关联资源（JSON字符串）
     */
    private String resources;

}
