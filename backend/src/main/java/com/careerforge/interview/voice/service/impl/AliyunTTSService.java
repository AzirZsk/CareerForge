package com.careerforge.interview.voice.service.impl;

import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtime;
import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtimeCallback;
import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtimeConfig;
import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtimeParam;
import com.careerforge.common.config.VoiceProperties;
import com.careerforge.interview.voice.enums.VoiceRole;
import com.careerforge.interview.voice.service.TTSListener;
import com.careerforge.interview.voice.service.TTSService;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 阿里云千问TTS实时语音合成服务实现
 * 基于 DashScope SDK 的 QwenTtsRealtime，采用 server_commit 模式
 * 对齐 ASR 的 start/sendAudio/close 生命周期模式
 *
 * @author Azir
 */
@Slf4j
public class AliyunTTSService implements TTSService {

    private static final String PROVIDER = "aliyun";
    private static final String WS_URL = "wss://dashscope.aliyuncs.com/api-ws/v1/realtime";

    private final VoiceProperties voiceProperties;
    private final VoiceRole role;
    // WebSocket 连接
    private QwenTtsRealtime connection;
    // 音频回调（connect 时一次性设置）
    private TTSListener listener;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

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
    public void connect(TTSListener listener) {
        if (connected.getAndSet(true)) {
            log.debug("[AliyunTTS] 连接已建立，跳过重复连接");
            return;
        }
        this.listener = listener;
        QwenTtsRealtimeParam param = buildParam();
        QwenTtsRealtimeCallback callback = new QwenTtsRealtimeCallback() {
            @Override
            public void onOpen() {
                log.info("[AliyunTTS] 连接已建立, role={}", role);
            }

            @Override
            public void onEvent(JsonObject message) {
                String type = message.get("type").getAsString();
                switch (type) {
                    case "response.audio.delta":
                        String audioB64 = message.get("delta").getAsString();
                        byte[] audioData = Base64.getDecoder().decode(audioB64);
                        if (AliyunTTSService.this.listener != null) {
                            AliyunTTSService.this.listener.onAudio(audioData);
                        }
                        break;
                    case "response.done":
                        log.trace("[AliyunTTS] 服务端处理完成");
                        break;
                    case "session.finished":
                        log.info("[AliyunTTS] 服务端结束会话, role={}", role);
                        close();
                        break;
                    case "error":
                        String errorMsg = message.has("error") ? message.get("error").getAsString() : "Unknown";
                        log.error("[AliyunTTS] 合成错误: {}, role={}", errorMsg, role);
                        if (AliyunTTSService.this.listener != null) {
                            AliyunTTSService.this.listener.onError(new RuntimeException("TTS error: " + errorMsg));
                        }
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onClose(int code, String reason) {
                log.info("[AliyunTTS] 连接关闭, code={}, reason={}, role={}", code, reason, role);
                closed.set(true);
                connected.set(false);
            }
        };
        connection = new QwenTtsRealtime(param, callback);
        try {
            connection.connect();
            QwenTtsRealtimeConfig sessionConfig = buildSessionConfig();
            connection.updateSession(sessionConfig);
            log.info("[AliyunTTS] 连接建立成功，server_commit 模式, role={}", role);
        } catch (Exception e) {
            connected.set(false);
            closed.set(true);
            throw new RuntimeException("TTS 连接建立失败", e);
        }
    }

    @Override
    public void synthesize(String text) {
        if (!connected.get() || closed.get()) {
            log.warn("[AliyunTTS] 连接未建立或已关闭，忽略合成请求, role={}", role);
            return;
        }
        try {
            connection.appendText(text);
        } catch (Exception e) {
            log.error("[AliyunTTS] 提交文本失败, role={}", role, e);
            if (listener != null) {
                listener.onError(e);
            }
        }
    }

    @Override
    public void close() {
        if (closed.getAndSet(true)) {
            return;
        }
        connected.set(false);
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                log.warn("[AliyunTTS] 关闭连接异常, role={}", role, e);
            }
        }
        log.info("[AliyunTTS] 连接已关闭, role={}", role);
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

    // ==================== 角色参数解析 ====================

    private String resolveVoice() {
        VoiceProperties.AliyunConfig.VoicesConfig voices = voiceProperties.getAliyun().getVoices();
        return role == VoiceRole.INTERVIEWER ? voices.getInterviewer() : voices.getAssistant();
    }

    private float resolveSpeechRate() {
        return role == VoiceRole.INTERVIEWER ? 1.0f : 1.1f;
    }

    private int resolveVolume() {
        return 80;
    }

    private float resolvePitchRate() {
        double pitch = role == VoiceRole.INTERVIEWER ? 0.0 : 0.1;
        return (float) (1.0 + pitch);
    }
}
