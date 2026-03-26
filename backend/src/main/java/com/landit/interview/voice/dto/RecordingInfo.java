package com.landit.interview.voice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
     * 文字记录条目
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TranscriptEntry {
        /**
         * 角色
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
