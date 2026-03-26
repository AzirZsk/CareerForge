package com.landit.interview.voice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TTS（语音合成）配置
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TTSConfig {

    /** TTS 模型 */
    private String model;

    /** 音色ID */
    private String voice;

    /** 输出格式：pcm, wav, mp3 */
    private String format;

    /** 采样率 */
    private Integer sampleRate;

    /** 语速 0.5-2.0 */
    private Double speechRate;

    /** 音量 0-1 */
    private Double volume;

    /** 音调 -1 到 1 */
    private Double pitch;

    /** 默认配置 */
    public static TTSConfig defaultConfig() {
        TTSConfig config = new TTSConfig();
        config.model = "cosyvoice-v2";
        config.format = "wav";
        config.sampleRate = 16000;
        config.speechRate = 1.0;
        config.volume = 0.8;
        config.pitch = 0.0;
        return config;
    }

    /** 面试官音色 */
    public static TTSConfig interviewer(String voiceId) {
        return withVoice(voiceId, 1.0, 0.0);
    }

    /** 助手音色 */
    public static TTSConfig assistant(String voiceId) {
        return withVoice(voiceId, 1.1, 0.1);
    }

    private static TTSConfig withVoice(String voiceId, double speechRate, double pitch) {
        TTSConfig config = new TTSConfig();
        config.model = "cosyvoice-v2";
        config.voice = voiceId;
        config.format = "wav";
        config.sampleRate = 16000;
        config.speechRate = speechRate;
        config.volume = 0.8;
        config.pitch = pitch;
        return config;
    }
}
