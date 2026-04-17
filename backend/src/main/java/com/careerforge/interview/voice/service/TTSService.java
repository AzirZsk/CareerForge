package com.careerforge.interview.voice.service;

import com.careerforge.interview.voice.dto.TTSConfig;

/**
 * TTS（语音合成）服务接口
 * 采用回调模式，音频数据通过 TTSListener 异步交付
 *
 * <p>实现类：
 * <ul>
 *   <li>{@code AliyunTTSService} - 阿里云千问TTS实时语音合成（DashScope SDK）</li>
 * </ul>
 *
 * @author Azir
 */
public interface TTSService {

    /**
     * 获取服务提供商标识
     *
     * @return "aliyun"
     */
    String getProvider();

    /**
     * 流式语音合成（单句）
     * 输入一段文本，音频片段通过 listener 异步回调交付
     *
     * @param text     要合成的文本
     * @param config   合成配置
     * @param listener 音频回调监听器
     */
    void streamSynthesize(String text, TTSConfig config, TTSListener listener);

    /**
     * 检查服务是否可用
     *
     * @return true 表示服务可用
     */
    default boolean isAvailable() {
        return true;
    }
}
