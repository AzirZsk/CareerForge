package com.careerforge.interview.voice.service.impl;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.utils.Constants;
import com.careerforge.common.config.VoiceProperties;
import com.careerforge.interview.voice.dto.ASRResult;
import com.careerforge.interview.voice.service.ASRListener;
import com.careerforge.interview.voice.service.ASRService;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 阿里云 Fun-ASR 实时语音识别会话实现
 * 每个面试会话创建一个实例，管理一条 WebSocket 长连接
 *
 * <p>SDK 回调直接通过 ASRListener 转发，无需 Flux 包装
 *
 * @author Azir
 */
@Slf4j
public class AliyunASRService implements ASRService {

    private final VoiceProperties voiceProperties;
    private final String sessionId;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean started = new AtomicBoolean(false);

    // 识别结果回调
    private ASRListener listener;
    // 阿里云识别器
    private Recognition recognizer;

    /**
     * 构造函数（由 VoiceServiceFactory 调用）
     *
     * @param voiceProperties 语音配置
     */
    public AliyunASRService(VoiceProperties voiceProperties) {
        this.voiceProperties = voiceProperties;
        this.sessionId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        // 设置北京地域 WebSocket URL
        Constants.baseWebsocketApiUrl = "wss://dashscope.aliyuncs.com/api-ws/v1/inference";
    }

    @Override
    public void start(ASRListener listener) {
        if (started.getAndSet(true)) {
            log.warn("[AliyunASR] 会话已启动，忽略重复调用, sessionId={}", sessionId);
            return;
        }
        this.listener = listener;

        VoiceProperties.AliyunConfig.ASRConfig asrConfig = voiceProperties.getAliyun().getAsr();
        this.recognizer = new Recognition();
        RecognitionParam param = buildRecognitionParam(asrConfig);

        // 构建 SDK 回调，直接转发给 ASRListener
        ResultCallback<RecognitionResult> callback = new ResultCallback<>() {
            @Override
            public void onEvent(RecognitionResult result) {
                ASRResult asrResult = convertToASRResult(result);
                if (!closed.get() && AliyunASRService.this.listener != null) {
                    AliyunASRService.this.listener.onResult(asrResult);
                }
                log.trace("[AliyunASR] 识别事件: isFinal={}, text={}, sessionId={}",
                        asrResult.getIsFinal(), asrResult.getText(), sessionId);
            }

            @Override
            public void onComplete() {
                log.info("[AliyunASR] 识别完成, sessionId={}", sessionId);
                markClosed();
                if (AliyunASRService.this.listener != null) {
                    AliyunASRService.this.listener.onComplete();
                }
            }

            @Override
            public void onError(Exception e) {
                log.error("[AliyunASR] 识别错误, sessionId={}", sessionId, e);
                markClosed();
                if (AliyunASRService.this.listener != null) {
                    AliyunASRService.this.listener.onError(e);
                }
            }
        };

        try {
            // 建立 WebSocket 连接
            recognizer.call(param, callback);
            log.info("[AliyunASR] 连接已建立, sessionId={}, model={}", sessionId, asrConfig.getModel());
        } catch (Exception e) {
            log.error("[AliyunASR] 建立连接失败, sessionId={}", sessionId, e);
            markClosed();
            if (this.listener != null) {
                this.listener.onError(e);
            }
        }
    }

    @Override
    public void sendAudio(byte[] audioFrame) {
        if (closed.get()) {
            log.warn("[AliyunASR] 会话已关闭，丢弃音频帧, sessionId={}", sessionId);
            return;
        }
        if (!started.get()) {
            log.warn("[AliyunASR] 会话未启动，丢弃音频帧, sessionId={}", sessionId);
            return;
        }
        try {
            recognizer.sendAudioFrame(ByteBuffer.wrap(audioFrame));
        } catch (Exception e) {
            log.error("[AliyunASR] 发送音频帧失败, sessionId={}", sessionId, e);
        }
    }

    @Override
    public void close() {
        closeInternal();
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public boolean isAvailable() {
        return isConfigured(voiceProperties);
    }

    /**
     * 检查阿里云 ASR 配置是否完整（静态方法，供工厂复用）
     *
     * @param voiceProperties 语音配置
     * @return true 表示配置完整可用
     */
    public static boolean isConfigured(VoiceProperties voiceProperties) {
        VoiceProperties.AliyunConfig aliyun = voiceProperties.getAliyun();
        return "aliyun".equals(voiceProperties.getProvider())
                && aliyun != null
                && aliyun.getApiKey() != null
                && !aliyun.getApiKey().isBlank();
    }

    /**
     * 安全关闭连接
     */
    private void closeInternal() {
        if (closed.getAndSet(true)) {
            return;
        }
        // 通知服务端结束识别
        if (recognizer != null) {
            try {
                recognizer.stop();
                log.debug("[AliyunASR] 已调用 stop, sessionId={}", sessionId);
            } catch (Exception e) {
                log.warn("[AliyunASR] stop 失败, sessionId={}", sessionId, e);
            }
            // 关闭 WebSocket 连接
            try {
                recognizer.getDuplexApi().close(1000, "bye");
                log.debug("[AliyunASR] 连接已关闭, sessionId={}", sessionId);
            } catch (Exception e) {
                log.warn("[AliyunASR] 关闭连接失败, sessionId={}", sessionId, e);
            }
        }
        log.info("[AliyunASR] 会话已关闭, sessionId={}", sessionId);
    }

    /**
     * 标记为已关闭（由 SDK 回调触发）
     */
    private void markClosed() {
        closed.set(true);
    }

    /**
     * 构建 Fun-ASR 识别参数
     */
    private RecognitionParam buildRecognitionParam(VoiceProperties.AliyunConfig.ASRConfig config) {
        return RecognitionParam.builder()
                .model(config.getModel())
                .apiKey(voiceProperties.getAliyun().getApiKey())
                .format(config.getFormat())
                .sampleRate(config.getSampleRate())
                // 语言提示
                .parameter("language_hints", new String[]{config.getLanguage()})
                // 标点预测
                .parameter("punctuation_prediction_enabled",
                        config.getEnablePunctuation() != null ? config.getEnablePunctuation() : true)
                .build();
    }

    /**
     * 将 SDK 的 RecognitionResult 转换为业务 ASRResult
     */
    private ASRResult convertToASRResult(RecognitionResult result) {
        return ASRResult.builder()
                .text(result.getSentence().getText())
                .isFinal(result.isSentenceEnd())
                .startTime(result.getSentence().getBeginTime())
                .endTime(result.getSentence().getEndTime())
                .build();
    }
}
