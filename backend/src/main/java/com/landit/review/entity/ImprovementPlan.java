package com.landit.review.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.landit.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 改进计划实体类
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_improvement_plan", autoResultMap = true)
public class ImprovementPlan extends BaseEntity {

    private Long reviewId;

    private String category;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> items;

}
