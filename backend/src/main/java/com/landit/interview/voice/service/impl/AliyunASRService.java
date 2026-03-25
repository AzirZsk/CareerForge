package com.landit.interview.voice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.common.config.VoiceProperties;
import com.landit.interview.voice.dto.ASRConfig;
import com.landit.interview.voice.dto.ASRResult;
import com.landit.interview.voice.service.ASRService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 阿里云 Paraformer 实时语音识别服务实现
 * 基于 WebSocket 实现流式语音识别
 *
 * <p>参考文档：https://help.aliyun.com/zh/model-studio/websocket-for-paraformer-real-time-service
 *
 * @author Azir
 */
@Slf4j
@Service("aliyunASRService")
public class AliyunASRService extends AliyunVoiceBaseService implements ASRService {

    private static final String PROVIDER = "aliyun";

    public AliyunASRService(VoiceProperties voiceProperties, ObjectMapper objectMapper) {
        super(voiceProperties, objectMapper);
    }

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    public Flux<ASRResult> streamRecognize(Flux<byte[]> audioStream, ASRConfig config) {
        // TODO: 实现 WebSocket 实时语音识别
        // 当前返回模拟结果，实际需要连接阿里云 Paraformer WebSocket API
        log.warn("[AliyunASR] streamRecognize not implemented yet, returning empty flux");
        return Flux.empty();
    }

    @Override
    public ASRResult recognize(byte[] audioData, ASRConfig config) {
        // TODO: 实现同步语音识别
        log.warn("[AliyunASR] recognize not implemented yet, returning empty result");
        return ASRResult.finalResult("");
    }

    @Override
    public boolean isAvailable() {
        return isApiKeyAvailable();
    }
}
