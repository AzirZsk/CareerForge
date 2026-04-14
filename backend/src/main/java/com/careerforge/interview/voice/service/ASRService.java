package com.careerforge.interview.voice.service;

import com.careerforge.interview.voice.dto.ASRResult;
import reactor.core.publisher.Flux;

/**
 * ASR（语音识别）会话服务
 * 每个面试会话创建一个独立实例，管理一条 WebSocket 长连接
 *
 * <p>生命周期：创建 -> start() -> [sendAudio x N] -> [results 持续发射] -> close()
 *
 * <p>使用示例：
 * <pre>{@code
 * ASRService asr = voiceServiceFactory.createASRService();
 * asr.start();
 *
 * // 订阅识别结果
 * asr.results().subscribe(result -> {
 *     if (result.getIsFinal()) {
 *         System.out.println("最终结果: " + result.getText());
 *     } else {
 *         System.out.println("中间结果: " + result.getText());
 *     }
 * });
 *
 * // 持续送入音频帧
 * asr.sendAudio(frame1);
 * asr.sendAudio(frame2);
 *
 * // 面试结束，关闭连接
 * asr.close();
 * }</pre>
 *
 * @author Azir
 */
public interface ASRService {

    /**
     * 建立识别连接
     * 创建 WebSocket 连接并开始接收识别结果
     * 调用后 results() 才会有数据输出
     */
    void start();

    /**
     * 发送一帧音频数据
     * 非阻塞操作，音频帧异步发送到服务端
     *
     * @param audioFrame PCM 音频数据（16kHz 16bit mono）
     */
    void sendAudio(byte[] audioFrame);

    /**
     * 获取识别结果流
     * 持续发射 ASRResult（中间结果 isFinal=false + 最终结果 isFinal=true）
     * close() 后此 Flux 收到 onComplete 信号
     *
     * @return 识别结果流
     */
    Flux<ASRResult> results();

    /**
     * 关闭连接，释放所有资源
     * 关闭后不可再调用 sendAudio 或 start
     */
    void close();

    /**
     * 连接是否已关闭
     *
     * @return true 表示已关闭
     */
    boolean isClosed();

    /**
     * 检查配置是否可用（用于健康检查）
     *
     * @return true 表示配置完整可用
     */
    boolean isAvailable();
}
