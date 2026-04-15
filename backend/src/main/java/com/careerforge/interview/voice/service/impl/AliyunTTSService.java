package com.careerforge.interview.voice.service.impl;

import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtime;
import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtimeCallback;
import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtimeConfig;
import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtimeParam;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.careerforge.common.config.VoiceProperties;
import com.careerforge.interview.voice.dto.TTSChunk;
import com.careerforge.interview.voice.dto.TTSConfig;
import com.careerforge.interview.voice.service.TTSService;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 阿里云千问TTS实时语音合成服务实现
 * 基于 DashScope SDK 的 QwenTtsRealtime 实现流式语音合成
 *
 * <p>参考文档：https://help.aliyun.com/zh/model-studio/qwen-tts-realtime
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class AliyunTTSService implements TTSService {

    private static final String PROVIDER = "aliyun";
    private static final String WS_URL = "wss://dashscope.aliyuncs.com/api-ws/v1/realtime";
    private static final String SENTENCE_END_CHARS = "。！？.!;；\n";

    private final VoiceProperties voiceProperties;

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    public Flux<byte[]> streamSynthesize(String text, TTSConfig config) {
        return Flux.<byte[]>create(emitter -> {
            String taskId = UUID.randomUUID().toString().replace("-", "");
            AtomicInteger audioChunkCount = new AtomicInteger(0);
            AtomicBoolean completed = new AtomicBoolean(false);
            CountDownLatch doneLatch = new CountDownLatch(1);
            // 构建连接参数和回调
            QwenTtsRealtimeParam param = buildParam(config);
            QwenTtsRealtimeCallback callback = new QwenTtsRealtimeCallback() {
                @Override
                public void onOpen() {
                    log.info("[AliyunTTS] WebSocket已连接, taskId={}", taskId);
                }

                @Override
                public void onEvent(JsonObject message) {
                    String type = message.get("type").getAsString();
                    switch (type) {
                        case "response.audio.delta":
                            String audioB64 = message.get("delta").getAsString();
                            byte[] audioData = Base64.getDecoder().decode(audioB64);
                            audioChunkCount.incrementAndGet();
                            if (!emitter.isCancelled()) {
                                emitter.next(audioData);
                            }
                            break;
                        case "response.done":
                            log.debug("[AliyunTTS] 响应完成, chunks={}, taskId={}", audioChunkCount.get(), taskId);
                            break;
                        case "session.finished":
                            log.info("[AliyunTTS] 会话结束, totalChunks={}, taskId={}", audioChunkCount.get(), taskId);
                            doneLatch.countDown();
                            safeComplete(emitter, completed);
                            break;
                        case "error":
                            String errorMsg = message.has("error")
                                    ? message.get("error").getAsString() : "Unknown TTS error";
                            log.error("[AliyunTTS] 合成错误: {}, taskId={}", errorMsg, taskId);
                            doneLatch.countDown();
                            if (!emitter.isCancelled() && completed.compareAndSet(false, true)) {
                                emitter.error(new RuntimeException("TTS error: " + errorMsg));
                            }
                            break;
                        default:
                            break;
                    }
                }

                @Override
                public void onClose(int code, String reason) {
                    log.info("[AliyunTTS] WebSocket关闭, code={}, reason={}, taskId={}", code, reason, taskId);
                    doneLatch.countDown();
                    safeComplete(emitter, completed);
                }
            };
            // 创建 TTS 客户端并注册资源释放钩子
            QwenTtsRealtime qwenTts = new QwenTtsRealtime(param, callback);
            emitter.onDispose(() -> {
                try {
                    qwenTts.close();
                } catch (Exception e) {
                    log.warn("[AliyunTTS] 关闭连接异常, taskId={}", taskId, e);
                }
            });
            // 建立连接并推送合成请求
            try {
                qwenTts.connect();
                QwenTtsRealtimeConfig sessionConfig = buildSessionConfig(config);
                qwenTts.updateSession(sessionConfig);
                qwenTts.appendText(text);
                qwenTts.finish();
                // 等待音频合成完成，超时30秒兜底
                if (!doneLatch.await(30, TimeUnit.SECONDS)) {
                    log.warn("[AliyunTTS] 等待合成超时, taskId={}", taskId);
                    safeComplete(emitter, completed);
                }
            } catch (NoApiKeyException e) {
                log.error("[AliyunTTS] API Key无效, taskId={}", taskId, e);
                if (!emitter.isCancelled() && completed.compareAndSet(false, true)) {
                    emitter.error(new RuntimeException("TTS API key not configured", e));
                }
            } catch (Exception e) {
                log.error("[AliyunTTS] 启动合成失败, taskId={}", taskId, e);
                if (!emitter.isCancelled() && completed.compareAndSet(false, true)) {
                    emitter.error(e);
                }
            }
        }, FluxSink.OverflowStrategy.BUFFER).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<TTSChunk> streamSynthesizeBySentence(Flux<String> textStream, TTSConfig config) {
        StringBuilder sentenceBuffer = new StringBuilder();
        // 按句子分割文本流，逐句合成音频
        return textStream.flatMap(delta -> {
            sentenceBuffer.append(delta);
            // 遇到句子结束符时立即合成当前句子
            if (isSentenceEnd(delta)) {
                String sentence = sentenceBuffer.toString();
                sentenceBuffer.setLength(0);
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
        VoiceProperties.AliyunConfig aliyun = voiceProperties.getAliyun();
        return aliyun != null && aliyun.getApiKey() != null && !aliyun.getApiKey().isBlank();
    }

    /**
     * 构建 QwenTtsRealtime 连接参数
     */
    private QwenTtsRealtimeParam buildParam(TTSConfig config) {
        VoiceProperties.AliyunConfig aliyun = voiceProperties.getAliyun();
        String model = config.getModel() != null ? config.getModel() : aliyun.getTts().getModel();
        return QwenTtsRealtimeParam.builder()
                .model(model)
                .url(WS_URL)
                .apikey(aliyun.getApiKey())
                .build();
    }

    /**
     * 构建 QwenTtsRealtime 会话配置
     * TTSConfig 参数映射到 QwenTtsRealtimeConfig
     */
    private QwenTtsRealtimeConfig buildSessionConfig(TTSConfig config) {
        VoiceProperties.AliyunConfig aliyun = voiceProperties.getAliyun();
        String voice = config.getVoice() != null ? config.getVoice() : aliyun.getVoices().getInterviewer();
        String format = config.getFormat() != null ? config.getFormat() : aliyun.getTts().getFormat();
        int sampleRate = config.getSampleRate() != null ? config.getSampleRate() : aliyun.getTts().getSampleRate();
        float speechRate = config.getSpeechRate() != null ? config.getSpeechRate().floatValue() : 1.0f;
        // volume: TTSConfig 0-1 映射到 QwenTtsRealtimeConfig 0-100
        int volume = config.getVolume() != null ? (int) (config.getVolume() * 100) : 50;
        // pitch: TTSConfig -1~1 映射到 QwenTtsRealtimeConfig pitchRate 0.5-2.0
        float pitchRate = config.getPitch() != null ? (float) (1.0 + config.getPitch()) : 1.0f;
        // 构建会话配置
        return QwenTtsRealtimeConfig.builder()
                .voice(voice)
                .format(format)
                .sampleRate(sampleRate)
                .speechRate(speechRate)
                .volume(volume)
                .pitchRate(pitchRate)
                .mode("server_commit")
                .build();
    }

    /**
     * 安全完成 Flux 发射器，保证只调用一次
     */
    private void safeComplete(FluxSink<byte[]> emitter, AtomicBoolean completed) {
        if (!emitter.isCancelled() && completed.compareAndSet(false, true)) {
            emitter.complete();
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
}
