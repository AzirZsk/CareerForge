package com.landit.interview.voice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ASR（语音识别）配置
 * 用于流式识别和同步识别的配置参数
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ASRConfig {

    /**
     * 音频格式：pcm, wav, mp3
     */
    private String format;

    /**
     * 采样率：16000, 24000
     */
    private Integer sampleRate;

    /**
     * 是否启用标点预测
     */
    private Boolean enablePunctuation;

    /**
     * 是否启用逆文本正则化（数字转阿拉伯）
     */
    private Boolean enableItn;

    /**
     * 语言：zh, en
     */
    private String language;

    /**
     * 是否启用语音活动检测
     */
    private Boolean enableVad;

    /**
     * 创建默认的 ASR 配置（16kHz PCM 中文）
     */
    public static ASRConfig defaultConfig() {
        return ASRConfig.builder()
                .format("pcm")
                .sampleRate(16000)
                .enablePunctuation(true)
                .enableItn(true)
                .language("zh")
                .enableVad(true)
                .build();
    }
}
