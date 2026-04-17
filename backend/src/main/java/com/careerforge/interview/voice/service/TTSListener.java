package com.careerforge.interview.voice.service;

/**
 * TTS 语音合成回调接口
 * 替代 Flux 响应式流，采用简单直接的回调模式
 *
 * @author Azir
 */
public interface TTSListener {

    /**
     * 收到音频数据片段
     *
     * @param audioData 音频字节数据（PCM/WAV 格式）
     */
    void onAudio(byte[] audioData);

    /**
     * 合成过程发生错误
     *
     * @param e 错误信息
     */
    void onError(Exception e);

    /**
     * 本次合成完成，所有音频已交付
     */
    void onComplete();
}
