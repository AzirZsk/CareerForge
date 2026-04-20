package com.careerforge.jobposition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.careerforge.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 职位实体类
 * 存储JD分析信息，同一公司同一职位可被多个面试复用
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_job_position", autoResultMap = true)
public class JobPosition extends BaseEntity {

    /**
     * 关联公司ID
     */
    private String companyId;

    /**
     * 职位名称
     */
    private String title;

    /**
     * JD原文
     */
    private String jdContent;

    /**
     * JD分析结果（JSON格式）
     */
    private String jdAnalysis;

    /**
     * 分析更新时间
     */
    private LocalDateTime jdAnalysisUpdatedAt;

    /**
     * 职位状态（用户手动设置）
     * draft/applied/interviewing/offered/rejected/withdrawn
     */
    private String status;

}
