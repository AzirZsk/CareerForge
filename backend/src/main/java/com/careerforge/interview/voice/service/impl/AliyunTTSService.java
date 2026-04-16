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
import java.util.function.Consumer;

/**
 * 阿里云千问TTS实时语音合成服务实现
 * 基于 DashScope SDK 的 QwenTtsRealtime 实现流式语音合成
 *
 * <p>参考文档：https://help.aliyun.com/zh/model-studio/qwen-tts-realtime
 *
 * @author Azir
 */
@Slf4j
public class AliyunTTSService implements TTSService {

    /**
     * 单例构造函数（Spring Bean 使用，无状态）
     */
    public AliyunTTSService(VoiceProperties voiceProperties) {
        this.voiceProperties = voiceProperties;
        this.defaultConfig = null;
    }

    /**
     * session-scoped 构造函数（VoiceServiceFactory.createTTSService 使用）
     * 每个面试会话独立创建，持有 WebSocket 长连接
     */
    public AliyunTTSService(VoiceProperties voiceProperties, TTSConfig defaultConfig) {
        this.voiceProperties = voiceProperties;
        this.defaultConfig = defaultConfig;
    }

    private static final String PROVIDER = "aliyun";
    private static final String WS_URL = "wss://dashscope.aliyuncs.com/api-ws/v1/realtime";
    private static final String SENTENCE_END_CHARS = "。！？.!;；\n";

    private final VoiceProperties voiceProperties;
    // session-scoped 连接复用相关字段（非单例实例才使用）
    private final TTSConfig defaultConfig;
    private QwenTtsRealtime persistentConnection;
    private FluxSink<byte[]> currentSink;
    private CountDownLatch responseDoneLatch;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    // 防止同一条连接上并发提交合成请求
    private final Object synthesisLock = new Object();

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

    // ==================== Session-Scoped 连接复用方法 ====================

    /**
     * 建立持久 WebSocket 连接（面试开始时调用一次）
     * 使用 commit 模式，支持在同一条连接上多轮 appendText + commit 合成
     */
    public void connect() {
        if (connected.getAndSet(true)) {
            log.debug("[AliyunTTS] 连接已建立，跳过重复连接");
            return;
        }
        TTSConfig config = defaultConfig != null ? defaultConfig : buildDefaultConfig();
        QwenTtsRealtimeParam param = buildParam(config);
        QwenTtsRealtimeCallback callback = new QwenTtsRealtimeCallback() {
            @Override
            public void onOpen() {
                log.info("[AliyunTTS] 持久连接已建立");
            }

            @Override
            public void onEvent(JsonObject message) {
                String type = message.get("type").getAsString();
                switch (type) {
                    case "response.audio.delta":
                        String audioB64 = message.get("delta").getAsString();
                        byte[] audioData = Base64.getDecoder().decode(audioB64);
                        if (currentSink != null && !currentSink.isCancelled()) {
                            currentSink.next(audioData);
                        }
                        break;
                    case "response.done":
                        // 单次合成完成，连接继续复用
                        if (responseDoneLatch != null) {
                            responseDoneLatch.countDown();
                        }
                        break;
                    case "session.finished":
                        log.info("[AliyunTTS] 服务端结束会话");
                        closeConnection();
                        break;
                    case "error":
                        String errorMsg = message.has("error") ? message.get("error").getAsString() : "Unknown";
                        log.error("[AliyunTTS] 持久连接合成错误: {}", errorMsg);
                        if (responseDoneLatch != null) {
                            responseDoneLatch.countDown();
                        }
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onClose(int code, String reason) {
                log.info("[AliyunTTS] 持久连接关闭, code={}, reason={}", code, reason);
                closed.set(true);
                connected.set(false);
            }
        };
        persistentConnection = new QwenTtsRealtime(param, callback);
        try {
            persistentConnection.connect();
            QwenTtsRealtimeConfig sessionConfig = buildSessionConfig(config);
            // 关键：使用 commit 模式而非 server_commit，支持多轮合成复用连接
            sessionConfig.setMode("commit");
            persistentConnection.updateSession(sessionConfig);
            log.info("[AliyunTTS] 持久连接建立成功，使用 commit 模式");
        } catch (Exception e) {
            connected.set(false);
            closed.set(true);
            throw new RuntimeException("TTS 持久连接建立失败", e);
        }
    }

    /**
     * 复用持久连接进行单句合成
     * 连接断开时自动降级到新建连接模式
     *
     * @param text 要合成的文本
     * @return 音频数据流
     */
    public Flux<byte[]> synthesizeWithConnection(String text) {
        // 连接不可用则降级到新建连接模式
        if (closed.get() || !connected.get()) {
            log.warn("[AliyunTTS] 持久连接不可用，降级到新建连接模式");
            return streamSynthesize(text, defaultConfig != null ? defaultConfig : buildDefaultConfig());
        }
        return Flux.<byte[]>create(emitter -> {
            synchronized (synthesisLock) {
                currentSink = emitter;
                responseDoneLatch = new CountDownLatch(1);
                emitter.onDispose(() -> currentSink = null);
                try {
                    persistentConnection.appendText(text);
                    persistentConnection.commit();
                    // 等待本次合成完成，超时30秒
                    if (!responseDoneLatch.await(30, TimeUnit.SECONDS)) {
                        log.warn("[AliyunTTS] 持久连接合成超时");
                    }
                } catch (Exception e) {
                    log.error("[AliyunTTS] 持久连接合成失败", e);
                    // 合成失败尝试关闭重置，下次调用会降级到新建连接
                    closeConnection();
                } finally {
                    currentSink = null;
                    emitter.complete();
                }
            }
        }, FluxSink.OverflowStrategy.BUFFER).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 复用持久连接的分句流式合成
     * 文本按句子分割后逐句通过同一条连接合成
     *
     * @param textStream 文本流（通常是 LLM 流式输出）
     * @return TTSChunk 流（文本 + 音频配对）
     */
    public Flux<TTSChunk> streamSynthesizeBySentenceWithConnection(Flux<String> textStream) {
        StringBuilder sentenceBuffer = new StringBuilder();
        // 按句子分割文本流，逐句通过持久连接合成音频
        return textStream.flatMap(delta -> {
            sentenceBuffer.append(delta);
            if (isSentenceEnd(delta)) {
                String sentence = sentenceBuffer.toString();
                sentenceBuffer.setLength(0);
                return synthesizeWithConnection(sentence)
                        .map(audio -> TTSChunk.of(sentence, audio));
            }
            return Flux.empty();
        }).concatWith(Flux.defer(() -> {
            // 处理剩余的文本
            if (sentenceBuffer.length() > 0) {
                String lastSentence = sentenceBuffer.toString();
                return synthesizeWithConnection(lastSentence)
                        .map(audio -> TTSChunk.finalChunk(lastSentence, audio));
            }
            return Flux.empty();
        }));
    }

    /**
     * 关闭持久连接（面试结束时调用）
     */
    public void closeConnection() {
        if (closed.getAndSet(true)) {
            return;
        }
        connected.set(false);
        if (persistentConnection != null) {
            try {
                persistentConnection.close();
            } catch (Exception e) {
                log.warn("[AliyunTTS] 关闭持久连接异常", e);
            }
        }
        log.info("[AliyunTTS] 持久连接已关闭");
    }

    /**
     * 连接是否已关闭
     */
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * 构建默认 TTS 配置（从 voiceProperties 读取）
     */
    private TTSConfig buildDefaultConfig() {
        VoiceProperties.AliyunConfig aliyun = voiceProperties.getAliyun();
        return TTSConfig.builder()
                .model(aliyun.getTts().getModel())
                .voice(aliyun.getVoices().getInterviewer())
                .format(aliyun.getTts().getFormat())
                .sampleRate(aliyun.getTts().getSampleRate())
                .speechRate(1.0)
                .volume(0.8)
                .pitch(0.0)
                .build();
    }
}
