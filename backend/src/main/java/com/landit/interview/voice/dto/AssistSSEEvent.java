package com.landit.interview.voice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSE 求助事件
 * 用于流式返回助手回复
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistSSEEvent {

    /**
     * 事件类型：text, audio, done, error
     */
    private String type;

    /**
     * 事件数据
     */
    private Object data;

    /**
     * 创建文本事件
     */
    public static AssistSSEEvent text(TextEventData data) {
        return AssistSSEEvent.builder().type("text").data(data).build();
    }

    /**
     * 创建音频事件
     */
    public static AssistSSEEvent audio(AudioEventData data) {
        return AssistSSEEvent.builder().type("audio").data(data).build();
    }

    /**
     * 创建完成事件
     */
    public static AssistSSEEvent done(DoneEventData data) {
        return AssistSSEEvent.builder().type("done").data(data).build();
    }

    /**
     * 创建错误事件
     */
    public static AssistSSEEvent error(ErrorEventData data) {
        return AssistSSEEvent.builder().type("error").data(data).build();
    }

    /**
     * 文本事件数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextEventData {
        /**
         * 文本内容（增量或完整）
         */
        private String content;

        /**
         * 是否增量（true=增量，false=完整）
         */
        private Boolean isDelta;
    }

    /**
     * 音频事件数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AudioEventData {
        /**
         * Base64 编码的音频数据
         */
        private String audio;

        /**
         * 音频格式：wav, pcm
         */
        private String format;

        /**
         * 采样率
         */
        private Integer sampleRate;
    }

    /**
     * 完成事件数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoneEventData {
        /**
         * 剩余求助次数
         */
        private Integer assistRemaining;

        /**
         * 总时长（毫秒）
         */
        private Integer totalDurationMs;
    }

    /**
     * 错误事件数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorEventData {
        /**
         * 错误码
         */
        private String code;

        /**
         * 错误消息
         */
        private String message;
    }
}
