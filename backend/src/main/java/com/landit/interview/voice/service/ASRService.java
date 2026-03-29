package com.landit.interview.voice.service;

import com.landit.interview.voice.dto.ASRConfig;
import com.landit.interview.voice.dto.ASRResult;
import reactor.core.publisher.Flux;

/**
 * ASR（语音识别）服务接口
 * 支持流式识别和同步识别，支持多服务商实现
 *
 * <p>实现类：
 * <ul>
 *   <li>{@code AliyunASRService} - 阿里云 Paraformer 实时语音识别</li>
 *   <li>{@code OpenAIASRService} - OpenAI Whisper（预留）</li>
 *   <li>{@code AzureASRService} - Azure Speech（预留）</li>
 * </ul>
 *
 * @author Azir
 */
public interface ASRService {

    /**
     * 获取服务提供商标识
     *
     * @return "aliyun", "openai", "azure" 等
     */
    String getProvider();

    /**
     * 流式语音识别（实时场景）
     * 适用于实时语音对话场景，边说边识别
     *
     * <p>使用示例：
     * <pre>{@code
     * Flux<byte[]> audioStream = ...; // 从麦克风采集的音频流
     * ASRConfig config = ASRConfig.defaultConfig();
     * Flux<ASRResult> results = asrService.streamRecognize(audioStream, config);
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
     * @param config      识别配置
     * @return 识别结果流，isFinal=false 为中间结果，isFinal=true 为最终结果
     */
    Flux<ASRResult> streamRecognize(Flux<byte[]> audioStream, ASRConfig config);

    /**
     * 同步语音识别（短音频）
     * 适用于短音频场景，如求助时的语音输入
     *
     * @param audioData 音频数据（完整的一段音频）
     * @param config    识别配置
     * @return 识别结果（总是最终结果）
     */
    ASRResult recognize(byte[] audioData, ASRConfig config);

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
