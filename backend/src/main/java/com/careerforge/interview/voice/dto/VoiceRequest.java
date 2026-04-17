package com.careerforge.interview.voice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 语音请求
 *
 * @author Azir
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoiceRequest<T> {

    /**
     * 消息类型：audio, control, ping
     */
    private String type;

    /**
     * 消息数据（泛型）
     */
    private T data;

    /**
     * 创建音频请求
     */
    public static VoiceRequest<AudioData> audio(AudioData audioData) {
        return new VoiceRequest<>("audio", audioData);
    }

    /**
     * 创建控制请求
     */
    public static VoiceRequest<ControlData> control(ControlData controlData) {
        return new VoiceRequest<>("control", controlData);
    }

    /**
     * 音频数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AudioData {
        /**
         * Base64 编码的音频数据
         */
        private String audio;

        /**
         * 音频格式：pcm, wav
         */
        private String format;

        /**
         * 采样率
         */
        private Integer sampleRate;
    }

    /**
     * 控制数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ControlData {
        /**
         * 控制动作：start, stop, end
         */
        private String action;
    }
}
