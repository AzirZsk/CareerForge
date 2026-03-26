package com.landit.interview.voice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.common.config.VoiceProperties;
import com.landit.interview.voice.dto.ASRConfig;
import com.landit.interview.voice.dto.ASRResult;
import com.landit.interview.voice.service.ASRService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
    private static final String WS_BASE_URL = "wss://dashscope.aliyuncs.com/api-ws/v1/inference/";

    private final HttpClient httpClient;

    public AliyunASRService(VoiceProperties voiceProperties, ObjectMapper objectMapper) {
        super(voiceProperties, objectMapper);
        this.httpClient = HttpClient.create();
    }

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    public Flux<ASRResult> streamRecognize(Flux<byte[]> audioStream, ASRConfig config) {
        return Flux.create(emitter -> {
            String taskId = UUID.randomUUID().toString().replace("-", "");
            AtomicBoolean connected = new AtomicBoolean(false);
            AtomicReference<reactor.netty.Connection> connectionRef = new AtomicReference<>();
            StringBuilder fullTextBuilder = new StringBuilder();

            try {
                // 构建 WebSocket URL
                String wsUrl = buildASRWebSocketUrl(config, taskId);
                log.info("[AliyunASR] Connecting to WebSocket for taskId={}", taskId);
                log.debug("[AliyunASR] WebSocket URL: {}", wsUrl.replaceAll("api-key=([^&]+)", "api-key=***"));

                // 建立 WebSocket 连接
                httpClient.websocket()
                        .uri(wsUrl)
                        .handle((inbound, outbound) -> {
                            connected.set(true);
                            log.info("[AliyunASR] WebSocket connected, taskId={}", taskId);

                            // 处理接收到的消息
                            Mono<Void> receiveMessages = inbound.receive()
                                    .aggregate()
                                    .asString()
                                    .doOnNext(payload -> parseASRResponse(payload, emitter, fullTextBuilder, taskId))
                                    .then();

                            // 发送音频流
                            Mono<Void> sendAudio = audioStream
                                    .flatMap(audioData -> outbound.sendObject(Mono.just(audioData)))
                                    .doOnComplete(() -> {
                                        log.info("[AliyunASR] Audio stream completed, taskId={}", taskId);
                                        sendFinishSignal(outbound, taskId);
                                    })
                                    .doOnError(error -> {
                                        log.error("[AliyunASR] Audio stream error, taskId={}", taskId, error);
                                        if (!emitter.isCancelled()) {
                                            emitter.error(error);
                                        }
                                    })
                                    .then();

                            return Mono.when(receiveMessages, sendAudio);
                        })
                        .doOnError(error -> {
                            log.error("[AliyunASR] WebSocket error, taskId={}", taskId, error);
                            if (!emitter.isCancelled()) {
                                emitter.error(error);
                            }
                        })
                        .doOnTerminate(() -> {
                            log.info("[AliyunASR] WebSocket terminated, taskId={}", taskId);
                            if (!emitter.isCancelled()) {
                                emitter.complete();
                            }
                        })
                        .subscribe();

            } catch (Exception e) {
                log.error("[AliyunASR] Failed to start recognition, taskId={}", taskId, e);
                if (!emitter.isCancelled()) {
                    emitter.error(e);
                }
            }

            // 清理资源
            emitter.onDispose(() -> {
                reactor.netty.Connection conn = connectionRef.get();
                if (conn != null && !conn.isDisposed()) {
                    conn.dispose();
                    log.info("[AliyunASR] WebSocket disposed, taskId={}", taskId);
                }
            });
        }, FluxSink.OverflowStrategy.BUFFER);
    }

    @Override
    public ASRResult recognize(byte[] audioData, ASRConfig config) {
        log.warn("[AliyunASR] Synchronous recognition is not optimized, consider using streamRecognize");
        try {
            return streamRecognize(Flux.just(audioData), config)
                    .filter(ASRResult::getIsFinal)
                    .takeLast(1)
                    .blockLast(Duration.ofSeconds(30));
        } catch (Exception e) {
            log.error("[AliyunASR] Synchronous recognition failed", e);
            return ASRResult.finalResult("");
        }
    }

    @Override
    public boolean isAvailable() {
        return isApiKeyAvailable();
    }

    /**
     * 构建 ASR WebSocket URL
     */
    private String buildASRWebSocketUrl(ASRConfig config, String taskId) {
        VoiceProperties.AliyunConfig aliyun = voiceProperties.getAliyun();
        String apiKey = aliyun.getApiKey();
        String model = config.getModel() != null ? config.getModel() : aliyun.getAsr().getModel();

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(WS_BASE_URL).append(model);
        urlBuilder.append("?api-key=").append(encodeUrl(apiKey));
        urlBuilder.append("&task_id=").append(taskId);
        urlBuilder.append("&model=").append(encodeUrl(model));
        urlBuilder.append("&format=").append(config.getFormat() != null ? config.getFormat() : aliyun.getAsr().getFormat());
        urlBuilder.append("&sample_rate=").append(config.getSampleRate() != null ? config.getSampleRate() : aliyun.getAsr().getSampleRate());
        urlBuilder.append("&enable_punctuation_prediction=").append(config.getEnablePunctuation() != null ? config.getEnablePunctuation() : aliyun.getAsr().getEnablePunctuation());
        urlBuilder.append("&enable_inverse_text_normalization=").append(config.getEnableItn() != null ? config.getEnableItn() : aliyun.getAsr().getEnableItn());
        urlBuilder.append("&enable_vad=").append(config.getEnableVad() != null ? config.getEnableVad() : aliyun.getAsr().getEnableVad());
        urlBuilder.append("&language=").append(config.getLanguage() != null ? config.getLanguage() : aliyun.getAsr().getLanguage());

        return urlBuilder.toString();
    }

    /**
     * 解析 ASR 响应
     */
    private void parseASRResponse(String payload, FluxSink<ASRResult> emitter,
                                  StringBuilder fullTextBuilder, String taskId) {
        try {
            JsonNode json = objectMapper.readTree(payload);
            String event = json.path("event").asText();

            log.trace("[AliyunASR] Received event: {}, taskId={}", event, taskId);

            switch (event) {
                case "result":
                    JsonNode output = json.path("output");
                    JsonNode sentence = output.path("sentence");
                    String text = sentence.path("text").asText();
                    boolean isFinal = sentence.path("end_time").asLong() > 0;

                    if (!text.isEmpty()) {
                        ASRResult result = ASRResult.builder()
                                .text(text)
                                .isFinal(isFinal)
                                .confidence(sentence.path("confidence").asDouble(0.0))
                                .startTime(sentence.path("begin_time").asLong(0L))
                                .endTime(sentence.path("end_time").asLong(0L))
                                .build();

                        if (!emitter.isCancelled()) {
                            emitter.next(result);
                        }

                        if (isFinal) {
                            fullTextBuilder.append(text);
                            log.debug("[AliyunASR] Final result: '{}', taskId={}", text, taskId);
                        } else {
                            log.trace("[AliyunASR] Partial result: '{}', taskId={}", text, taskId);
                        }
                    }
                    break;

                case "completed":
                    log.info("[AliyunASR] Recognition completed, full text length={}, taskId={}",
                            fullTextBuilder.length(), taskId);
                    break;

                case "error":
                    String errorCode = json.path("error_code").asText();
                    String errorMessage = json.path("error_message").asText();
                    log.error("[AliyunASR] Recognition error: {} - {}, taskId={}", errorCode, errorMessage, taskId);
                    if (!emitter.isCancelled()) {
                        emitter.error(new RuntimeException("ASR error: " + errorCode + " - " + errorMessage));
                    }
                    break;

                default:
                    log.trace("[AliyunASR] Unknown event: {}, taskId={}", event, taskId);
            }
        } catch (Exception e) {
            log.error("[AliyunASR] Failed to parse message: {}, taskId={}", payload, taskId, e);
        }
    }

    /**
     * 发送结束信号
     */
    private void sendFinishSignal(reactor.netty.http.websocket.WebsocketOutbound outbound, String taskId) {
        try {
            String finishMessage = "{\"action\":\"finish\"}";
            outbound.sendString(Mono.just(finishMessage))
                    .then()
                    .subscribe(
                            v -> log.info("[AliyunASR] Sent finish signal, taskId={}", taskId),
                            e -> log.warn("[AliyunASR] Failed to send finish signal", e)
                    );
        } catch (Exception e) {
            log.warn("[AliyunASR] Failed to send finish signal", e);
        }
    }

    private String encodeUrl(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}
