package com.careerforge.interview.voice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TTS（语音合成）配置
 * 实际配置值由 VoiceProperties 提供，通过 Builder 构建
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
}
