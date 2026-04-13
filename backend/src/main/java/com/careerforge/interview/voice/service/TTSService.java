package com.careerforge.interview.voice.service;

import com.careerforge.interview.voice.dto.TTSChunk;
import com.careerforge.interview.voice.dto.TTSConfig;
import reactor.core.publisher.Flux;

/**
 * TTS（语音合成）服务接口
 * 仅支持流式合成，适用于实时语音对话场景
 *
 * <p>实现类：
 * <ul>
 *   <li>{@code AliyunTTSService} - 阿里云 CosyVoice 流式语音合成</li>
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
     * 输入一段文本，流式输出音频片段
     *
     * <p>使用示例：
     * <pre>{@code
     * String text = "你好，欢迎参加面试。";
     * TTSConfig config = TTSConfig.interviewer("longxiaochun_v2");
     * Flux<byte[]> audioStream = ttsService.streamSynthesize(text, config);
     * audioStream.subscribe(audioChunk -> {
     *     // 播放音频片段
     *     playAudio(audioChunk);
     * });
     * }</pre>
     *
     * @param text   要合成的文本
     * @param config 合成配置
     * @return 音频片段流（PCM/WAV 格式）
     */
    Flux<byte[]> streamSynthesize(String text, TTSConfig config);

    /**
     * 分句流式合成（长文本，边生成边播放）
     * 将 LLM 输出的文本流按句子分割，逐句合成，实现真正的流式播放
     *
     * <p>这是语音面试场景的核心方法，能够实现：
     * <ul>
     *   <li>LLM 生成一句 → TTS 合成一句 → 播放一句</li>
     *   <li>大幅降低首字节延迟</li>
     *   <li>用户体验更流畅</li>
     * </ul>
     *
     * <p>使用示例：
     * <pre>{@code
     * Flux<String> llmStream = llmService.streamGenerate(prompt); // LLM 输出
     * TTSConfig config = TTSConfig.interviewer("longxiaochun_v2");
     * Flux<TTSChunk> chunks = ttsService.streamSynthesizeBySentence(llmStream, config);
     * chunks.subscribe(chunk -> {
     *     // 显示文本
     *     displayText(chunk.getText());
     *     // 播放音频
     *     playAudio(chunk.getAudio());
     * });
     * }</pre>
     *
     * @param textStream 文本流（通常是 LLM 的流式输出）
     * @param config     合成配置
     * @return 合成块流（包含文本和音频）
     */
    Flux<TTSChunk> streamSynthesizeBySentence(Flux<String> textStream, TTSConfig config);

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
