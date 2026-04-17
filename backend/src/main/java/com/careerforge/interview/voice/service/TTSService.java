package com.careerforge.interview.voice.service;

import com.careerforge.interview.voice.dto.TTSConfig;

/**
 * TTS（语音合成）服务接口
 * 采用 connect/synthesize/close 生命周期模式，音频数据通过 TTSListener 异步回调交付
 *
 * <p>使用方式：
 * <ul>
 *   <li>无状态模式：直接调用 {@link #synthesize}，每次新建临时连接</li>
 *   <li>会话模式：{@link #connect} 建立持久连接 -> 多次 {@link #synthesize} 复用连接 -> {@link #close} 关闭</li>
 * </ul>
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
     * 检查服务是否可用
     *
     * @return true 表示服务可用
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * 建立持久连接（会话模式）
     * 无状态实现可忽略此方法（使用 default 空实现）
     */
    default void connect() {
    }

    /**
     * 合成语音
     * 如果已调用 {@link #connect}，则复用持久连接；否则创建临时连接
     *
     * @param text     要合成的文本
     * @param listener 音频回调监听器
     */
    void synthesize(String text, TTSListener listener);

    /**
     * 关闭连接并释放资源
     * 无状态实现可忽略此方法（使用 default 空实现）
     */
    default void close() {
    }
}
