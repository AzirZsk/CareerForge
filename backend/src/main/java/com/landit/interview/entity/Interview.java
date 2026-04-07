package com.landit.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.landit.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

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

    private LocalDateTime date;

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

    /**
     * 关联简历ID（用于面试准备参考）
     */
    private String resumeId;

    /**
     * 轮次类型（technical_1/technical_2/hr/director/cto/final/custom）
     */
    private String roundType;

    /**
     * 自定义轮次名称（仅当 roundType=custom 时使用）
     */
    private String roundName;

    // ===== 面试类型与地址信息 =====

    /**
     * 面试类型（onsite-现场 / online-线上）
     */
    private String interviewType;

    /**
     * 现场面试地址（仅当 interviewType='onsite' 时有值）
     */
    private String location;

    /**
     * 线上面试链接（仅当 interviewType='online' 时有值）
     */
    private String onlineLink;

    /**
     * 线上面试密码/会议号（如 Zoom/腾讯会议的会议密码）
     */
    private String meetingPassword;

    /**
     * 面试过程文字记录（手动输入或音频转录）
     */
    private String transcript;

}
