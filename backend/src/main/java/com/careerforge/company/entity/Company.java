package com.careerforge.company.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.careerforge.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 公司实体类
 * 存储公司调研信息，同一公司可被多个面试复用
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_company", autoResultMap = true)
public class Company extends BaseEntity {

    /**
     * 公司名称
     */
    private String name;

    /**
     * 公司调研结果（JSON格式）
     */
    private String research;

    /**
     * 调研更新时间
     */
    private LocalDateTime researchUpdatedAt;

}
