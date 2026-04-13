package com.landit.interview.voice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.common.config.VoiceProperties;
import com.landit.interview.voice.dto.TTSChunk;
import com.landit.interview.voice.dto.TTSConfig;
import com.landit.interview.voice.service.TTSService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
    private static final String WS_BASE_URL = "wss://dashscope.aliyuncs.com/api-ws/v1/inference/";
    private static final String SENTENCE_END_CHARS = "。！？.!;；\n";

    private final HttpClient httpClient;

    public AliyunTTSService(VoiceProperties voiceProperties, ObjectMapper objectMapper) {
        super(voiceProperties, objectMapper);
        this.httpClient = HttpClient.create();
    }

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    public Flux<byte[]> streamSynthesize(String text, TTSConfig config) {
        return Flux.create(emitter -> {
            String taskId = UUID.randomUUID().toString().replace("-", "");
            AtomicBoolean connected = new AtomicBoolean(false);
            AtomicReference<reactor.netty.Connection> connectionRef = new AtomicReference<>();
            AtomicInteger audioChunkCount = new AtomicInteger(0);

            try {
                // 构建 WebSocket URL
                String wsUrl = buildTTSWebSocketUrl(config, taskId);
                log.info("[AliyunTTS] 正在连接WebSocket, textLength={}, taskId={}",
                        text.length(), taskId);
                log.debug("[AliyunTTS] WebSocket URL: {}", wsUrl.replaceAll("api-key=([^&]+)", "api-key=***"));

                // 建立 WebSocket 连接
                httpClient.websocket()
                        .uri(wsUrl)
                        .handle((inbound, outbound) -> {
                            connected.set(true);
                            log.info("[AliyunTTS] WebSocket已连接, taskId={}", taskId);

                            // 发送 TTS 请求
                            String request = buildTTSRequest(text, config, taskId);
                            Mono<Void> sendRequest = outbound.sendString(Mono.just(request))
                                    .then()
                                    .doOnSuccess(v -> log.debug("[AliyunTTS] 已发送TTS请求, taskId={}", taskId));

                            // 处理接收到的消息
                            Mono<Void> receiveMessages = inbound.receive()
                                    .aggregate()
                                    .asString()
                                    .doOnNext(payload -> handleTextMessage(payload, emitter, taskId, audioChunkCount))
                                    .then();

                            return sendRequest.then(receiveMessages);
                        })
                        .doOnError(error -> {
                            log.error("[AliyunTTS] WebSocket错误, taskId={}", taskId, error);
                            if (!emitter.isCancelled()) {
                                emitter.error(error);
                            }
                        })
                        .doOnTerminate(() -> {
                            log.info("[AliyunTTS] WebSocket结束, totalChunks={}, taskId={}",
                                    audioChunkCount.get(), taskId);
                            if (!emitter.isCancelled()) {
                                emitter.complete();
                            }
                        })
                        .subscribe();

            } catch (Exception e) {
                log.error("[AliyunTTS] 启动合成失败, taskId={}", taskId, e);
                if (!emitter.isCancelled()) {
                    emitter.error(e);
                }
            }

            // 清理资源
            emitter.onDispose(() -> {
                reactor.netty.Connection conn = connectionRef.get();
                if (conn != null && !conn.isDisposed()) {
                    conn.dispose();
                    log.info("[AliyunTTS] WebSocket已释放, taskId={}", taskId);
                }
            });
        }, FluxSink.OverflowStrategy.BUFFER);
    }

    @Override
    public Flux<TTSChunk> streamSynthesizeBySentence(Flux<String> textStream, TTSConfig config) {
        StringBuilder sentenceBuffer = new StringBuilder();

        return textStream.flatMap(delta -> {
            sentenceBuffer.append(delta);

            if (isSentenceEnd(delta)) {
                String sentence = sentenceBuffer.toString();
                sentenceBuffer.setLength(0);

                // 流式合成这个句子
                return streamSynthesize(sentence, config)
                        .map(audio -> TTSChunk.of(sentence, audio));
            }
            return Flux.empty();
        }).concatWith(Flux.defer(() -> {
            // 处理剩余的文本
            if (sentenceBuffer.length() > 0) {
                String lastSentence = sentenceBuffer.toString();
                return streamSynthesize(lastSentence, config)
                        .map(audio -> TTSChunk.finalChunk(lastSentence, audio));
            }
            return Flux.empty();
        }));
    }

    @Override
    public boolean isAvailable() {
        return isApiKeyAvailable();
    }

    /**
     * 构建 TTS WebSocket URL
     */
    private String buildTTSWebSocketUrl(TTSConfig config, String taskId) {
        VoiceProperties.AliyunConfig aliyun = voiceProperties.getAliyun();
        String apiKey = aliyun.getApiKey();
        String model = config.getModel() != null ? config.getModel() : aliyun.getTts().getModel();

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(WS_BASE_URL).append(model);
        urlBuilder.append("?api-key=").append(encodeUrl(apiKey));
        urlBuilder.append("&task_id=").append(taskId);
        urlBuilder.append("&model=").append(encodeUrl(model));

        return urlBuilder.toString();
    }

    /**
     * 构建 TTS 请求 JSON
     */
    private String buildTTSRequest(String text, TTSConfig config, String taskId) {
        try {
            VoiceProperties.AliyunConfig aliyun = voiceProperties.getAliyun();

            Map<String, Object> request = new HashMap<>();
            request.put("text", text);
            request.put("voice", config.getVoice() != null ? config.getVoice() : aliyun.getVoices().getInterviewer());
            request.put("format", config.getFormat() != null ? config.getFormat() : aliyun.getTts().getFormat());
            request.put("sample_rate", config.getSampleRate() != null ? config.getSampleRate() : aliyun.getTts().getSampleRate());
            request.put("rate", config.getSpeechRate() != null ? config.getSpeechRate() : 1.0);
            request.put("volume", config.getVolume() != null ? (int)(config.getVolume() * 100) : 50);
            request.put("pitch", config.getPitch() != null ? config.getPitch() : 0.0);

            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            log.error("[AliyunTTS] 构建TTS请求失败", e);
            throw new RuntimeException("Failed to build TTS request", e);
        }
    }

    /**
     * 处理 WebSocket 文本消息
     */
    private void handleTextMessage(String payload, FluxSink<byte[]> emitter,
                                   String taskId, AtomicInteger audioChunkCount) {
        try {
            JsonNode json = objectMapper.readTree(payload);
            String event = json.path("event").asText();

            log.trace("[AliyunTTS] 收到事件: {}, taskId={}", event, taskId);

            switch (event) {
                case "result":
                    JsonNode output = json.path("output");
                    if (output.has("audio")) {
                        String audioBase64 = output.path("audio").asText();
                        if (!audioBase64.isEmpty()) {
                            byte[] audioData = Base64.getDecoder().decode(audioBase64);
                            audioChunkCount.incrementAndGet();
                            log.trace("[AliyunTTS] 收到音频块, size={}字节, taskId={}",
                                    audioData.length, taskId);
                            if (!emitter.isCancelled()) {
                                emitter.next(audioData);
                            }
                        }
                    }
                    break;

                case "completed":
                    log.info("[AliyunTTS] 合成完成, totalChunks={}, taskId={}",
                            audioChunkCount.get(), taskId);
                    break;

                case "error":
                    String errorCode = json.path("error_code").asText();
                    String errorMessage = json.path("error_message").asText();
                    log.error("[AliyunTTS] 合成错误: {} - {}, taskId={}", errorCode, errorMessage, taskId);
                    if (!emitter.isCancelled()) {
                        emitter.error(new RuntimeException("TTS error: " + errorCode + " - " + errorMessage));
                    }
                    break;

                default:
                    log.trace("[AliyunTTS] 未知事件: {}, taskId={}", event, taskId);
            }
        } catch (Exception e) {
            log.error("[AliyunTTS] 解析消息失败, taskId={}", taskId, e);
        }
    }

    /**
     * 判断是否句子结束
     */
    private boolean isSentenceEnd(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        char lastChar = text.charAt(text.length() - 1);
        return SENTENCE_END_CHARS.indexOf(lastChar) >= 0;
    }

    private String encodeUrl(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}
