package com.careerforge.interview.voice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 录音片段 DTO
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordingSegment {

    /**
     * 片段序号
     */
    private Integer index;

    /**
     * 角色：interviewer, candidate, assistant
     * @see com.careerforge.interview.voice.enums.TranscriptRole
     */
    private String role;

    /**
     * 文字内容
     */
    private String content;

    /**
     * 音频数据（仅在传输时使用）
     */
    private byte[] audioData;

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
