package com.landit.interview.voice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 录音回放信息 DTO
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordingInfo {

    /**
     * 会话 ID
     */
    private String sessionId;

    /**
     * 总时长（毫秒）
     */
    private Integer totalDurationMs;

    /**
     * 合并后的音频 URL
     */
    private String mergedAudioUrl;

    /**
     * 录音片段列表
     */
    private List<RecordingSegment> segments;

    /**
     * 文字记录列表
     */
    private List<TranscriptEntry> transcript;

    /**
     * 录音片段
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordingSegment {
        /**
         * 片段序号
         */
        private Integer index;

        /**
         * 角色
         * @see com.landit.interview.voice.enums.TranscriptRole
         */
        private String role;

        /**
         * 内容
         */
        private String content;

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

        /**
         * 音频 URL
         */
        private String audioUrl;
    }

    /**
     * 文字记录条目
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TranscriptEntry {
        /**
         * 角色
         * @see com.landit.interview.voice.enums.TranscriptRole
         */
        private String role;

        /**
         * 内容
         */
        private String content;

        /**
         * 时间戳（毫秒）
         */
        private Long timestamp;

        /**
         * 片段序号
         */
        private Integer segmentIndex;
    }
}
