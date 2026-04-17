package com.careerforge.interview.voice.service;

/**
 * ASR（语音识别）会话服务
 * 每个面试会话创建一个独立实例，管理一条 WebSocket 长连接
 *
 * <p>生命周期：创建 -> start(listener) -> [sendAudio x N] -> [listener 持续回调] -> close()
 *
 * <p>使用示例：
 * <pre>{@code
 * ASRService asr = voiceServiceFactory.createASRService();
 * asr.start(new ASRListener() {
 *     public void onResult(ASRResult result) {
 *         if (result.getIsFinal()) {
 *             System.out.println("最终结果: " + result.getText());
 *         }
 *     }
 *     public void onError(Exception e) { e.printStackTrace(); }
 *     public void onComplete() { System.out.println("识别结束"); }
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
     * 建立识别连接并注册回调
     * 创建 WebSocket 连接，识别结果通过 listener 异步回调
     *
     * @param listener 识别结果回调
     */
    void start(ASRListener listener);

    /**
     * 发送一帧音频数据
     * 非阻塞操作，音频帧异步发送到服务端
     *
     * @param audioFrame PCM 音频数据（16kHz 16bit mono）
     */
    void sendAudio(byte[] audioFrame);

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
