package com.careerforge.interview.voice.service;

/**
 * TTS（语音合成）服务接口
 * 采用 connect/synthesize/close 生命周期模式，对齐 ASR 的 start/sendAudio/close 模式
 * 音频数据通过 TTSListener 异步回调交付
 *
 * <p>使用方式：
 * <pre>
 *   tts.connect(listener);          // 建立连接 + 设置回调
 *   tts.synthesize(text, false);        // 非阻塞提交文本，音频通过 listener 异步回传
 *   tts.synthesize(lastText, true);     // 最后一段文本，立即 commit 触发合成
 *   tts.close();                    // 关闭连接
 * </pre>
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
     * 建立连接并设置音频回调监听器
     * 创建 WebSocket 连接，设置 server_commit 模式，保存 listener 用于后续音频回传
     *
     * @param listener 音频回调监听器（onAudio 接收音频数据，onError 接收错误）
     */
    void connect(TTSListener listener);

    /**
     * 提交文本进行语音合成（非阻塞）
     * server_commit 模式下服务端自动检测句子边界并开始合成，音频通过 connect 时设置的 listener 异步回传
     *
     * @param text  要合成的文本（完整句子或 LLM delta 片段均可）
     * @param isEnd 是否为最后一段文本，true 时立即 commit 触发 TTS 合成缓冲区中的所有文本
     */
    void synthesize(String text, boolean isEnd);

    /**
     * 关闭连接并释放资源
     */
    void close();
}
