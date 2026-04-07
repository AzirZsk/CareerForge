package com.landit.interview.voice.service.impl;

import com.alibaba.dashscope.audio.asr.transcription.Transcription;
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionParam;
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionResult;
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionTaskResult;
import com.alibaba.dashscope.common.TaskStatus;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.landit.common.config.VoiceProperties;
import com.landit.interview.voice.dto.FileASRResult;
import com.landit.interview.voice.service.FileASRService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 阿里云 Paraformer 录音文件识别服务实现
 * 基于 DashScope SDK 的 Transcription API
 *
 * <p>注意：阿里云 Transcription API 不支持本地文件直传，需要公网可访问的 URL。
 * 本实现先将文件保存到本地，然后通过服务器的公网 URL 提供 API 访问。
 *
 * <p>参考文档：https://help.aliyun.com/zh/model-studio/paraformer-recorded-speech-recognition-java-sdk
 *
 * <p>支持格式：wav/mp3/m4a/aac/ogg/flac/opus 等
 * <p>支持时长：无限制（最大 12 小时）
 *
 * @author Azir
 */
@Slf4j
@Service("aliyunFileASRService")
public class AliyunFileASRService implements FileASRService {

    private static final String PROVIDER = "aliyun";

    /**
     * 支持的音频格式
     */
    private static final Set<String> SUPPORTED_FORMATS = Set.of(
            "wav", "mp3", "m4a", "aac", "ogg", "flac", "opus", "wma", "ape", "flv",
            "mov", "mp4", "mpeg", "avi", "mkv", "webm", "amr"
    );

    private final VoiceProperties voiceProperties;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final Gson gson = new Gson();

    @Value("${landit.voice.aliyun.file-asr.storage-path:./data/audio-uploads}")
    private String storagePath;

    @Value("${landit.voice.aliyun.file-asr.public-url-base:http://localhost:8080/landit/audio-files}")
    private String publicUrlBase;

    public AliyunFileASRService(VoiceProperties voiceProperties) {
        this.voiceProperties = voiceProperties;
    }

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    public reactor.core.publisher.Flux<FileASRResult> transcribe(MultipartFile file) {
        return reactor.core.publisher.Flux.create(emitter -> {
            String taskId = UUID.randomUUID().toString().substring(0, 8);
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String savedFilename = taskId + "." + fileExtension;

            log.info("[FileASR] 开始转录: taskId={}, filename={}, size={}bytes",
                    taskId, originalFilename, file.getSize());

            // 校验文件格式
            if (!isSupportedFormat(fileExtension)) {
                emitter.next(FileASRResult.failed("不支持的音频格式: " + fileExtension +
                        "，支持 wav/mp3/m4a/aac/ogg/flac 等格式"));
                emitter.complete();
                return;
            }

            // 校验文件大小
            int maxSizeMb = voiceProperties.getAliyun().getFileAsr().getMaxFileSizeMb();
            long fileSizeMb = file.getSize() / (1024 * 1024);
            if (fileSizeMb > maxSizeMb) {
                emitter.next(FileASRResult.failed("文件大小超出限制: 最大 " + maxSizeMb + "MB，当前 " + fileSizeMb + "MB"));
                emitter.complete();
                return;
            }

            try {
                // 发送开始事件
                emitter.next(FileASRResult.processing("正在保存音频文件...", 5));

                // 1. 保存文件到本地
                Path savedPath = saveFile(file, savedFilename);
                log.info("[FileASR] 文件已保存: taskId={}, path={}", taskId, savedPath);

                // 2. 构建公网可访问的 URL
                String fileUrl = buildPublicUrl(savedFilename);
                log.info("[FileASR] 文件URL: {}", fileUrl);

                emitter.next(FileASRResult.processing("正在提交转录任务...", 10));

                // 3. 创建 Transcription 客户端并提交任务
                Transcription transcription = new Transcription();
                TranscriptionParam param = buildTranscriptionParam(fileUrl);

                TranscriptionResult submitResult = transcription.asyncCall(param);
                String transcriptionTaskId = submitResult.getTaskId();
                log.info("[FileASR] 任务已提交: taskId={}, transcriptionTaskId={}",
                        taskId, transcriptionTaskId);

                emitter.next(FileASRResult.processing("音频文件已提交，正在转录...", 15));

                // 4. 轮询任务状态
                pollTaskStatus(transcription, transcriptionTaskId, emitter, taskId, savedPath);

            } catch (IOException e) {
                log.error("[FileASR] 保存文件失败: taskId={}", taskId, e);
                emitter.next(FileASRResult.failed("保存文件失败: " + e.getMessage()));
                emitter.complete();
            } catch (Exception e) {
                log.error("[FileASR] 提交任务失败: taskId={}", taskId, e);
                emitter.next(FileASRResult.failed("提交转录任务失败: " + e.getMessage()));
                emitter.complete();
            }

        }, reactor.core.publisher.FluxSink.OverflowStrategy.BUFFER);
    }

    /**
     * 保存文件到本地存储
     */
    private Path saveFile(MultipartFile file, String filename) throws IOException {
        Path uploadDir = Paths.get(storagePath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        Path filePath = uploadDir.resolve(filename);
        file.transferTo(filePath.toFile());
        return filePath;
    }

    /**
     * 构建公网可访问的 URL
     */
    private String buildPublicUrl(String filename) {
        return publicUrlBase + "/" + filename;
    }

    /**
     * 轮询任务状态
     */
    private void pollTaskStatus(Transcription transcription, String transcriptionTaskId,
                                 reactor.core.publisher.FluxSink<FileASRResult> emitter,
                                 String taskId, Path savedFilePath) {
        long pollingInterval = voiceProperties.getAliyun().getFileAsr().getPollingIntervalMs();
        final int[] progressCounter = {20};
        final int[] retryCount = {0};
        final int maxRetries = 60; // 最多轮询 60 次（约 2 分钟）

        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (emitter.isCancelled()) {
                    cleanupFile(savedFilePath);
                    scheduler.shutdown();
                    return;
                }

                retryCount[0]++;
                if (retryCount[0] > maxRetries) {
                    emitter.next(FileASRResult.failed("转录超时，请稍后重试"));
                    emitter.complete();
                    cleanupFile(savedFilePath);
                    scheduler.shutdown();
                    return;
                }

                // 查询任务状态
                TranscriptionResult result = transcription.fetch(
                        com.alibaba.dashscope.audio.asr.transcription.TranscriptionQueryParam
                                .FromTranscriptionParam(null, transcriptionTaskId)
                );

                TaskStatus status = result.getTaskStatus();
                log.debug("[FileASR] 任务状态: taskId={}, status={}", taskId, status);

                if (status == TaskStatus.SUCCEEDED) {
                    // 转录成功
                    emitter.next(FileASRResult.processing("转录完成，正在整理结果...", 90));

                    // 提取转录文本
                    String transcriptText = extractTranscriptText(result);
                    log.info("[FileASR] 转录完成: taskId={}, textLength={}", taskId, transcriptText.length());

                    emitter.next(FileASRResult.completed(transcriptText));
                    emitter.complete();
                    cleanupFile(savedFilePath);
                    scheduler.shutdown();

                } else if (status == TaskStatus.FAILED) {
                    // 转录失败
                    String errorMsg = "转录失败";
                    // 尝试从结果中获取错误信息
                    if (result.getOutput() != null) {
                        try {
                            JsonObject output = gson.fromJson(result.getOutput(), JsonObject.class);
                            if (output.has("message")) {
                                errorMsg = output.get("message").getAsString();
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    log.error("[FileASR] 转录失败: taskId={}, error={}", taskId, errorMsg);
                    emitter.next(FileASRResult.failed(errorMsg));
                    emitter.complete();
                    cleanupFile(savedFilePath);
                    scheduler.shutdown();

                } else {
                    // 处理中（PENDING/RUNNING）
                    progressCounter[0] = Math.min(progressCounter[0] + 2, 80);
                    String statusMsg = status == TaskStatus.PENDING ? "排队中..." : "正在转录...";
                    emitter.next(FileASRResult.processing(statusMsg, progressCounter[0]));
                }

            } catch (Exception e) {
                log.error("[FileASR] 轮询状态异常: taskId={}", taskId, e);
                // 不立即失败，继续重试
            }
        }, pollingInterval, pollingInterval, TimeUnit.MILLISECONDS);
    }

    /**
     * 构建转录参数
     */
    private TranscriptionParam buildTranscriptionParam(String fileUrl) {
        VoiceProperties.AliyunConfig.FileASRConfig config = voiceProperties.getAliyun().getFileAsr();

        return TranscriptionParam.builder()
                .model(config.getModel())
                .apiKey(voiceProperties.getAliyun().getApiKey())
                .parameter("language_hints", new String[]{config.getLanguage()})
                .fileUrls(Collections.singletonList(fileUrl))
                .build();
    }

    /**
     * 从转录结果中提取文本
     */
    private String extractTranscriptText(TranscriptionResult result) {
        StringBuilder sb = new StringBuilder();
        // 获取子任务结果
        java.util.List<TranscriptionTaskResult> taskResults = result.getResults();
        if (taskResults != null) {
            for (TranscriptionTaskResult taskResult : taskResults) {
                // 获取转录 URL 并下载结果
                String transcriptionUrl = taskResult.getTranscriptionUrl();
                if (transcriptionUrl != null) {
                    try {
                        String transcriptContent = downloadTranscriptionResult(transcriptionUrl);
                        String text = parseTranscriptionText(transcriptContent);
                        if (!text.isBlank()) {
                            sb.append(text).append("\n");
                        }
                    } catch (Exception e) {
                        log.warn("[FileASR] 下载转录结果失败: {}", transcriptionUrl, e);
                    }
                }
            }
        }
        return sb.toString().trim();
    }

    /**
     * 下载转录结果
     */
    private String downloadTranscriptionResult(String url) throws IOException {
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    /**
     * 解析转录结果 JSON，提取文本
     */
    private String parseTranscriptionText(String jsonContent) {
        StringBuilder sb = new StringBuilder();
        try {
            JsonObject root = gson.fromJson(jsonContent, JsonObject.class);
            if (root.has("transcripts")) {
                for (var transcript : root.getAsJsonArray("transcripts")) {
                    if (transcript.getAsJsonObject().has("sentences")) {
                        for (var sentence : transcript.getAsJsonObject().getAsJsonArray("sentences")) {
                            if (sentence.getAsJsonObject().has("text")) {
                                sb.append(sentence.getAsJsonObject().get("text").getAsString()).append("\n");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[FileASR] 解析转录结果失败", e);
        }
        return sb.toString().trim();
    }

    /**
     * 清理临时文件
     */
    private void cleanupFile(Path filePath) {
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.debug("[FileASR] 已清理临时文件: {}", filePath);
            }
        } catch (IOException e) {
            log.warn("[FileASR] 清理临时文件失败: {}", filePath, e);
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(dotIndex + 1).toLowerCase() : "";
    }

    /**
     * 检查是否支持的格式
     */
    private boolean isSupportedFormat(String extension) {
        return SUPPORTED_FORMATS.contains(extension);
    }

    @Override
    public boolean isAvailable() {
        VoiceProperties.AliyunConfig aliyun = voiceProperties.getAliyun();
        return aliyun != null
                && aliyun.getApiKey() != null
                && !aliyun.getApiKey().isBlank();
    }
}
