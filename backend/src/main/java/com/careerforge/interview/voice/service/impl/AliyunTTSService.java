package com.careerforge.interview.voice.service.impl;

import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtime;
import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtimeCallback;
import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtimeConfig;
import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtimeParam;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.careerforge.common.config.VoiceProperties;
import com.careerforge.interview.voice.enums.VoiceRole;
import com.careerforge.interview.voice.service.TTSListener;
import com.careerforge.interview.voice.service.TTSService;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 阿里云千问TTS实时语音合成服务实现
 * 基于 DashScope SDK 的 QwenTtsRealtime 实现流式语音合成
 * 支持两种模式：
 * 1. 无状态模式：直接调用 synthesize()，每次创建临时连接
 * 2. 会话模式：connect() 建立持久连接 -> 多次 synthesize() 复用 -> close() 关闭
 *
 * @author Azir
 */
@Slf4j
public class AliyunTTSService implements TTSService {

    private static final String PROVIDER = "aliyun";
    private static final String WS_URL = "wss://dashscope.aliyuncs.com/api-ws/v1/realtime";

    private final VoiceProperties voiceProperties;
    private final VoiceRole role;
    // session-scoped 连接复用相关字段
    private QwenTtsRealtime persistentConnection;
    private TTSListener currentListener;
    private CountDownLatch responseDoneLatch;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    // 防止同一条连接上并发提交合成请求
    private final Object synthesisLock = new Object();

    /**
     * 构造函数
     *
     * @param voiceProperties 语音配置属性
     * @param role            语音角色（面试官/助手），决定音色和语速等参数
     */
    public AliyunTTSService(VoiceProperties voiceProperties, VoiceRole role) {
        this.voiceProperties = voiceProperties;
        this.role = role;
    }

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    public boolean isAvailable() {
        VoiceProperties.AliyunConfig aliyun = voiceProperties.getAliyun();
        return aliyun != null && aliyun.getApiKey() != null && !aliyun.getApiKey().isBlank();
    }

    @Override
    public void connect() {
        if (connected.getAndSet(true)) {
            log.debug("[AliyunTTS] 连接已建立，跳过重复连接");
            return;
        }
        QwenTtsRealtimeParam param = buildParam();
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
                        if (currentListener != null) {
                            currentListener.onAudio(audioData);
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
                        close();
                        break;
                    case "error":
                        String errorMsg = message.has("error") ? message.get("error").getAsString() : "Unknown";
                        log.error("[AliyunTTS] 持久连接合成错误: {}", errorMsg);
                        if (responseDoneLatch != null) {
                            responseDoneLatch.countDown();
                        }
                        if (currentListener != null) {
                            currentListener.onError(new RuntimeException("TTS error: " + errorMsg));
                            currentListener = null;
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
            QwenTtsRealtimeConfig sessionConfig = buildSessionConfig();
            // 使用 service_commit 模式，服务端自动提交，无需手动 commit
            sessionConfig.setMode("service_commit");
            persistentConnection.updateSession(sessionConfig);
            log.info("[AliyunTTS] 持久连接建立成功，使用 service_commit 模式");
        } catch (Exception e) {
            connected.set(false);
            closed.set(true);
            throw new RuntimeException("TTS 持久连接建立失败", e);
        }
    }

    @Override
    public void synthesize(String text, TTSListener listener) {
        // 会话模式：已建立持久连接，复用连接合成
        if (connected.get() && !closed.get()) {
            synthesizeWithConnection(text, listener);
        } else {
            // 无状态模式：每次创建临时连接
            synthesizeWithTemporaryConnection(text, listener);
        }
    }

    @Override
    public void close() {
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

    // ==================== 内部合成方法 ====================

    /**
     * 复用持久连接进行单句合成
     */
    private void synthesizeWithConnection(String text, TTSListener listener) {
        CompletableFuture.runAsync(() -> {
            synchronized (synthesisLock) {
                currentListener = listener;
                responseDoneLatch = new CountDownLatch(1);
                try {
                    persistentConnection.appendText(text);
                    // 等待本次合成完成，超时30秒
                    if (!responseDoneLatch.await(30, TimeUnit.SECONDS)) {
                        log.warn("[AliyunTTS] 持久连接合成超时");
                    }
                } catch (Exception e) {
                    log.error("[AliyunTTS] 持久连接合成失败", e);
                    listener.onError(e);
                    // 合成失败尝试关闭重置，下次调用会降级到临时连接
                    close();
                } finally {
                    currentListener = null;
                    listener.onComplete();
                }
            }
        });
    }

    /**
     * 创建临时连接进行单次合成（无状态模式）
     */
    private void synthesizeWithTemporaryConnection(String text, TTSListener listener) {
        CompletableFuture.runAsync(() -> {
            String taskId = UUID.randomUUID().toString().replace("-", "");
            AtomicBoolean completed = new AtomicBoolean(false);
            CountDownLatch doneLatch = new CountDownLatch(1);
            // 构建连接参数和回调
            QwenTtsRealtimeParam param = buildParam();
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
                            listener.onAudio(audioData);
                            break;
                        case "response.done":
                            log.debug("[AliyunTTS] 响应完成, taskId={}", taskId);
                            break;
                        case "session.finished":
                            log.info("[AliyunTTS] 会话结束, taskId={}", taskId);
                            doneLatch.countDown();
                            safeComplete(listener, completed);
                            break;
                        case "error":
                            String errorMsg = message.has("error")
                                    ? message.get("error").getAsString() : "Unknown TTS error";
                            log.error("[AliyunTTS] 合成错误: {}, taskId={}", errorMsg, taskId);
                            doneLatch.countDown();
                            if (completed.compareAndSet(false, true)) {
                                listener.onError(new RuntimeException("TTS error: " + errorMsg));
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
                    safeComplete(listener, completed);
                }
            };
            // 创建 TTS 客户端
            QwenTtsRealtime qwenTts = new QwenTtsRealtime(param, callback);
            try {
                // 建立连接并推送合成请求
                qwenTts.connect();
                QwenTtsRealtimeConfig sessionConfig = buildSessionConfig();
                qwenTts.updateSession(sessionConfig);
                qwenTts.appendText(text);
                qwenTts.finish();
                // 等待音频合成完成，超时30秒兜底
                if (!doneLatch.await(30, TimeUnit.SECONDS)) {
                    log.warn("[AliyunTTS] 等待合成超时, taskId={}", taskId);
                    safeComplete(listener, completed);
                }
            } catch (NoApiKeyException e) {
                log.error("[AliyunTTS] API Key无效, taskId={}", taskId, e);
                if (completed.compareAndSet(false, true)) {
                    listener.onError(new RuntimeException("TTS API key not configured", e));
                }
            } catch (Exception e) {
                log.error("[AliyunTTS] 启动合成失败, taskId={}", taskId, e);
                if (completed.compareAndSet(false, true)) {
                    listener.onError(e);
                }
            } finally {
                try {
                    qwenTts.close();
                } catch (Exception e) {
                    log.warn("[AliyunTTS] 关闭连接异常, taskId={}", taskId, e);
                }
            }
        });
    }

    // ==================== 内部工具方法 ====================

    /**
     * 构建 QwenTtsRealtime 连接参数
     */
    private QwenTtsRealtimeParam buildParam() {
        VoiceProperties.AliyunConfig aliyun = voiceProperties.getAliyun();
        return QwenTtsRealtimeParam.builder()
                .model(aliyun.getTts().getModel())
                .url(WS_URL)
                .apikey(aliyun.getApiKey())
                .build();
    }

    /**
     * 构建 QwenTtsRealtime 会话配置，根据角色解析音色和参数
     */
    private QwenTtsRealtimeConfig buildSessionConfig() {
        VoiceProperties.AliyunConfig aliyun = voiceProperties.getAliyun();
        return QwenTtsRealtimeConfig.builder()
                .voice(resolveVoice())
                .format(aliyun.getTts().getFormat())
                .sampleRate(aliyun.getTts().getSampleRate())
                .speechRate(resolveSpeechRate())
                .volume(resolveVolume())
                .pitchRate(resolvePitchRate())
                .mode("server_commit")
                .languageType("Chinese")
                .build();
    }

    /**
     * 安全完成回调，保证 onComplete 只调用一次
     */
    private void safeComplete(TTSListener listener, AtomicBoolean completed) {
        if (completed.compareAndSet(false, true)) {
            listener.onComplete();
        }
    }

    // ==================== 角色参数解析 ====================

    private String resolveVoice() {
        VoiceProperties.AliyunConfig.VoicesConfig voices = voiceProperties.getAliyun().getVoices();
        return role == VoiceRole.INTERVIEWER ? voices.getInterviewer() : voices.getAssistant();
    }

    private float resolveSpeechRate() {
        return role == VoiceRole.INTERVIEWER ? 1.0f : 1.1f;
    }

    private int resolveVolume() {
        // volume 0-1 映射到 QwenTtsRealtimeConfig 0-100
        return 80;
    }

    private float resolvePitchRate() {
        // pitch -1~1 映射到 pitchRate 0.5-2.0
        double pitch = role == VoiceRole.INTERVIEWER ? 0.0 : 0.1;
        return (float) (1.0 + pitch);
    }
}
