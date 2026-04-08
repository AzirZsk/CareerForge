package com.landit.interview.voice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 预生成问题
 * 缓存预生成的面试问题，用于零延迟推送
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreGeneratedQuestion {

    /**
     * 问题序号（0 = 开场白+第1题）
     */
    private Integer questionIndex;

    /**
     * 问题文本
     */
    private String text;

    /**
     * TTS 音频数据（可选，全语音模式用）
     */
    private byte[] audioData;

    /**
     * 音频格式
     */
    private String audioFormat;

    /**
     * 采样率
     */
    private Integer sampleRate;

    /**
     * 是否已使用
     */
    private boolean used = false;
}
