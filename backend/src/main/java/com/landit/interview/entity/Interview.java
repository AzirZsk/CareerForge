package com.landit.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.landit.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 面试记录实体类
 * 支持真实面试（Real）和模拟面试（Mock）两种来源
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_interview", autoResultMap = true)
public class Interview extends BaseEntity {

    private String userId;

    private String type;

    private String position;

    private String company;

    private LocalDate date;

    private Integer duration;

    private Integer score;

    private String status;

    private Integer questions;

    private Integer correctAnswers;

    // ===== 新增字段：真实面试生命周期管理 =====

    private String source;

    private String jdContent;

    private String overallResult;

    private String notes;

    private String companyResearch;

    private String jdAnalysis;

    /**
     * 关联职位ID（指向 t_job_position 表）
     */
    private String jobPositionId;

}
