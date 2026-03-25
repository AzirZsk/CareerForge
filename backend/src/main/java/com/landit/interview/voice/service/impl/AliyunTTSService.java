package com.landit.interview.voice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.common.config.VoiceProperties;
import com.landit.interview.voice.dto.TTSChunk;
import com.landit.interview.voice.dto.TTSConfig;
import com.landit.interview.voice.service.TTSService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

/**
 * 阿里云 CosyVoice 流式语音合成服务实现
 * 基于 WebSocket 实现流式语音合成
 *
 * <p>参考文档：https://help.aliyun.com/zh/model-studio/cosyvoice-clone-design-api
 *
 * @author Azir
 */
@Slf4j
@Service("aliyunTTSService")
public class AliyunTTSService extends AliyunVoiceBaseService implements TTSService {

    private static final String PROVIDER = "aliyun";
    private static final String SENTENCE_END_CHARS = "。！？.!;；\n";

    public AliyunTTSService(VoiceProperties voiceProperties, ObjectMapper objectMapper) {
        super(voiceProperties, objectMapper);
    }

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    public Flux<byte[]> streamSynthesize(String text, TTSConfig config) {
        // TODO: 实现 WebSocket 流式语音合成
        // 当前返回空流，实际需要连接阿里云 CosyVoice WebSocket API
        log.warn("[AliyunTTS] streamSynthesize not implemented yet for text length={}", text.length());
        return Flux.empty();
    }

    @Override
    public Flux<TTSChunk> streamSynthesizeBySentence(Flux<String> textStream, TTSConfig config) {
        StringBuilder sentenceBuffer = new StringBuilder();

        return textStream.flatMap(delta -> {
            sentenceBuffer.append(delta);

            if (isSentenceEnd(delta)) {
                String sentence = sentenceBuffer.toString();
                sentenceBuffer.setLength(0);
                return streamSynthesize(sentence, config)
                        .map(audio -> TTSChunk.of(sentence, audio));
            }
            return Flux.empty();
        }).concatWith(Flux.defer(() -> {
            if (sentenceBuffer.length() > 0) {
                String lastSentence = sentenceBuffer.toString();
                return streamSynthesize(lastSentence, config)
                        .map(audio -> TTSChunk.finalChunk(lastSentence, audio));
            }
            return Flux.empty();
        }));
    }

    @Override
    public byte[] synthesize(String text, TTSConfig config) {
        // TODO: 实现同步语音合成
        log.warn("[AliyunTTS] synthesize not implemented yet for text length={}", text.length());
        return new byte[0];
    }

    @Override
    public boolean isAvailable() {
        return isApiKeyAvailable();
    }

    private boolean isSentenceEnd(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        char lastChar = text.charAt(text.length() - 1);
        return SENTENCE_END_CHARS.indexOf(lastChar) >= 0;
    }
}
