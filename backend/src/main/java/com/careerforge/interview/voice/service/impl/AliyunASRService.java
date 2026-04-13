package com.careerforge.interview.voice.service.impl;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.utils.Constants;
import com.careerforge.common.config.VoiceProperties;
import com.careerforge.interview.voice.dto.ASRResult;
import com.careerforge.interview.voice.service.ASRService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * 阿里云 Fun-ASR 实时语音识别服务实现
 * 基于 DashScope SDK 官方封装，简化 WebSocket 管理
 *
 * <p>参考文档：https://help.aliyun.com/zh/model-studio/real-time-speech-recognition-fun-asr
 *
 * @author Azir
 */
@Slf4j
@Service("aliyunASRService")
public class AliyunASRService implements ASRService {

    private static final String PROVIDER = "aliyun";

    private final VoiceProperties voiceProperties;

    public AliyunASRService(VoiceProperties voiceProperties) {
        this.voiceProperties = voiceProperties;
        // 设置北京地域 WebSocket URL（新加坡地域用 dashscope-intl.aliyuncs.com）
        Constants.baseWebsocketApiUrl = "wss://dashscope.aliyuncs.com/api-ws/v1/inference";
    }

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    /**
     * 流式语音识别
     * 将音频流实时发送到 Fun-ASR 服务，返回识别结果流
     *
     * @param audioStream PCM 音频流（16kHz）
     * @return 识别结果流（包含中间结果和最终结果）
     */
    @Override
    public Flux<ASRResult> streamRecognize(Flux<byte[]> audioStream) {
        VoiceProperties.AliyunConfig.ASRConfig asrConfig = voiceProperties.getAliyun().getAsr();

        return Flux.create(emitter -> {
            Recognition recognizer = new Recognition();
            String taskId = UUID.randomUUID().toString().replace("-", "");
            RecognitionParam param = buildRecognitionParam(asrConfig);

            // 构建回调处理器，将 SDK 事件转换为 Flux 元素
            ResultCallback<RecognitionResult> callback = new ResultCallback<>() {
                @Override
                public void onEvent(RecognitionResult result) {
                    ASRResult asrResult = convertToASRResult(result);
                    if (!emitter.isCancelled()) {
                        emitter.next(asrResult);
                    }
                    log.trace("[AliyunASR] 识别事件: isFinal={}, text={}, taskId={}",
                            asrResult.getIsFinal(), asrResult.getText(), taskId);
                }

                @Override
                public void onComplete() {
                    log.info("[AliyunASR] 识别完成, taskId={}", taskId);
                    if (!emitter.isCancelled()) {
                        emitter.complete();
                    }
                    closeRecognizer(recognizer, taskId);
                }

                @Override
                public void onError(Exception e) {
                    log.error("[AliyunASR] 识别错误, taskId={}", taskId, e);
                    if (!emitter.isCancelled()) {
                        emitter.error(e);
                    }
                    closeRecognizer(recognizer, taskId);
                }
            };

            try {
                recognizer.call(param, callback);
                log.info("[AliyunASR] 识别开始, taskId={}, model={}", taskId, asrConfig.getModel());

                // 订阅音频流，将每帧音频发送到识别器
                audioStream.subscribe(
                    audioData -> recognizer.sendAudioFrame(ByteBuffer.wrap(audioData)),
                    error -> {
                        log.error("[AliyunASR] 音频流错误, taskId={}", taskId, error);
                        if (!emitter.isCancelled()) {
                            emitter.error(error);
                        }
                    },
                    () -> {
                        log.info("[AliyunASR] 音频流结束，停止识别, taskId={}", taskId);
                        recognizer.stop();
                    }
                );
            } catch (Exception e) {
                log.error("[AliyunASR] 启动识别失败, taskId={}", taskId, e);
                if (!emitter.isCancelled()) {
                    emitter.error(e);
                }
            }

            emitter.onDispose(() -> closeRecognizer(recognizer, taskId));
        }, FluxSink.OverflowStrategy.BUFFER);
    }

    @Override
    public boolean isAvailable() {
        VoiceProperties.AliyunConfig aliyun = voiceProperties.getAliyun();
        return aliyun != null && aliyun.getApiKey() != null && !aliyun.getApiKey().isBlank();
    }

    /**
     * 构建 Fun-ASR 识别参数
     * 所有配置从 application.yml 读取，无需运行时传参
     */
    private RecognitionParam buildRecognitionParam(VoiceProperties.AliyunConfig.ASRConfig config) {
        return RecognitionParam.builder()
                .model(config.getModel())
                .apiKey(voiceProperties.getAliyun().getApiKey())
                .format(config.getFormat())
                .sampleRate(config.getSampleRate())
                // 语言提示（数组形式，首个元素生效）
                .parameter("language_hints", new String[]{config.getLanguage()})
                // 标点预测（默认开启）
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

    /**
     * 安全关闭识别器
     */
    private void closeRecognizer(Recognition recognizer, String taskId) {
        try {
            recognizer.getDuplexApi().close(1000, "bye");
            log.debug("[AliyunASR] 识别器已关闭, taskId={}", taskId);
        } catch (Exception e) {
            log.warn("[AliyunASR] 关闭识别器失败, taskId={}", taskId, e);
        }
    }
}
