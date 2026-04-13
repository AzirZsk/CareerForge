package com.careerforge.interview.voice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.careerforge.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 面试录音片段实体
 *
 * @author Azir
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("t_interview_recording")
public class InterviewRecording extends BaseEntity {

    /**
     * 关联的面试会话 ID
     */
    private String sessionId;

    /**
     * 片段序号
     */
    private Integer segmentIndex;

    /**
     * 角色：interviewer / candidate / assistant
     */
    private String role;

    /**
     * 对应的文字内容
     */
    private String content;

    /**
     * 音频文件路径
     */
    private String audioPath;

    /**
     * 时长（毫秒）
     */
    private Integer durationMs;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;
}
