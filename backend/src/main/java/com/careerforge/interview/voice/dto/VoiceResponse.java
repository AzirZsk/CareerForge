package com.careerforge.interview.voice.dto;

import static com.careerforge.interview.voice.enums.ResponseTypeEnum.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 语音响应
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceResponse {

    /**
     * 消息类型
     */
    private String type;

    /**
     * 消息数据
     */
    private Object data;

    /**
     * 创建转录响应
     */
    public static VoiceResponse transcript(TranscriptData data) {
        return VoiceResponse.builder().type(TRANSCRIPT.getCode()).data(data).build();
    }

    /**
     * 创建音频响应
     */
    public static VoiceResponse audio(AudioData data) {
        return VoiceResponse.builder().type(AUDIO.getCode()).data(data).build();
    }

    /**
     * 创建状态响应
     */
    public static VoiceResponse state(StateData data) {
        return VoiceResponse.builder().type(STATE.getCode()).data(data).build();
    }

    /**
     * 创建错误响应
     */
    public static VoiceResponse error(ErrorData data) {
        return VoiceResponse.builder().type(ERROR.getCode()).data(data).build();
    }

    /**
     * 创建心跳响应
     */
    public static VoiceResponse pong() {
        return VoiceResponse.builder().type(PONG.getCode()).build();
    }

    /**
     * 创建准备就绪响应（预生成完成）
     */
    public static VoiceResponse ready(ReadyData data) {
        return VoiceResponse.builder().type(READY.getCode()).data(data).build();
    }

    /**
     * 转录数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TranscriptData {
        /**
         * 转录文本
         */
        private String text;

        /**
         * 是否最终结果
         */
        private Boolean isFinal;

        /**
         * 角色：interviewer, candidate, assistant
         * @see com.careerforge.interview.voice.enums.TranscriptRole
         */
        private String role;

        /**
         * 置信度
         */
        private Double confidence;
    }

    /**
     * 音频数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AudioData {
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
     * 状态数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StateData {
        /**
         * 会话状态：interviewing, frozen, completed
         */
        private String state;

        /**
         * 当前问题序号
         */
        private Integer currentQuestion;

        /**
         * 总问题数
         */
        private Integer totalQuestions;

        /**
         * 剩余求助次数
         */
        private Integer assistRemaining;

        /**
         * 已用时间（秒）
         */
        private Integer elapsedTime;
    }

    /**
     * 错误数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorData {
        /**
         * 错误码
         */
        private String code;

        /**
         * 错误消息
         */
        private String message;
    }

    /**
     * 准备就绪数据（预生成完成通知）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReadyData {
        /**
         * 就绪状态：ready
         */
        private String state;

        /**
         * 提示消息
         */
        private String message;
    }
}
