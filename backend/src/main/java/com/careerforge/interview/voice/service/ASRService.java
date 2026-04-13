package com.careerforge.interview.voice.service;

import com.careerforge.interview.voice.dto.ASRResult;
import reactor.core.publisher.Flux;

/**
 * ASR（语音识别）服务接口
 * 仅支持流式识别，适用于实时语音对话场景
 *
 * <p>实现类：
 * <ul>
 *   <li>{@code AliyunASRService} - 阿里云 Fun-ASR 实时语音识别</li>
 * </ul>
 *
 * @author Azir
 */
public interface ASRService {

    /**
     * 获取服务提供商标识
     *
     * @return "aliyun"
     */
    String getProvider();

    /**
     * 流式语音识别（实时场景）
     * 适用于实时语音对话场景，边说边识别
     *
     * <p>配置从 application.yml 读取，无需运行时传参：
     * <pre>
     * careerforge.voice.aliyun.asr.model: fun-asr-realtime-2026-02-28
     * careerforge.voice.aliyun.asr.format: pcm
     * careerforge.voice.aliyun.asr.sample-rate: 16000
     * careerforge.voice.aliyun.asr.language: zh
     * </pre>
     *
     * <p>使用示例：
     * <pre>{@code
     * Flux<byte[]> audioStream = ...; // 从麦克风采集的音频流
     * Flux<ASRResult> results = asrService.streamRecognize(audioStream);
     * results.subscribe(result -> {
     *     if (result.getIsFinal()) {
     *         System.out.println("最终结果: " + result.getText());
     *     } else {
     *         System.out.println("中间结果: " + result.getText());
     *     }
     * });
     * }</pre>
     *
     * @param audioStream 音频流（PCM 格式，16kHz）
     * @return 识别结果流，isFinal=false 为中间结果，isFinal=true 为最终结果
     */
    Flux<ASRResult> streamRecognize(Flux<byte[]> audioStream);

    /**
     * 检查服务是否可用
     * 用于健康检查或启动时验证配置
     *
     * @return true 表示服务可用
     */
    default boolean isAvailable() {
        return true;
    }
}
