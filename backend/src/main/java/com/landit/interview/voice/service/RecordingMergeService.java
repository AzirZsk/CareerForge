package com.landit.interview.voice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.interview.voice.dto.RecordingInfo;
import com.landit.interview.voice.entity.InterviewRecording;
import com.landit.interview.voice.entity.RecordingIndex;
import com.landit.interview.voice.mapper.InterviewRecordingMapper;
import com.landit.interview.voice.mapper.RecordingIndexMapper;
import com.landit.interview.voice.util.WavHeaderUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 录音合并服务
 * 处理面试录音的合并和管理
 *
 * @author Azir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecordingMergeService {

    private final InterviewRecordingMapper recordingMapper;
    private final RecordingIndexMapper indexMapper;
    private final ObjectMapper objectMapper;

    @Value("${landit.recording.storage-path:./data/recordings}")
    private String storagePath;

    // 合并进度缓存：sessionId -> progress (0-100)
    private final ConcurrentHashMap<String, Integer> mergeProgressMap = new ConcurrentHashMap<>();

    /**
     * 合并会话的所有录音片段
     * 将多个 PCM 片段合并为一个 WAV 文件
     *
     * @param sessionId 会话 ID
     * @return 合并后的录音信息
     */
    public Mono<RecordingInfo> mergeRecordings(String sessionId) {
        log.info("[RecordingMerge] 开始合并, sessionId={}", sessionId);
        return Mono.fromCallable(() -> doMergeRecordings(sessionId))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 流式返回合并后的音频数据
     *
     * @param sessionId 会话 ID
     * @return 音频数据流
     */
    public Flux<byte[]> streamMergedAudio(String sessionId) {
        return Mono.fromCallable(() -> {
                    Path mergedPath = Paths.get(storagePath, sessionId, "merged.wav");
                    if (!Files.exists(mergedPath)) {
                        // 如果没有合并文件，先合并
                        mergeRecordings(sessionId).block();
                    }
                    return mergedPath;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(this::streamAudioInChunks);
    }

    /**
     * 检查是否已合并
     *
     * @param sessionId 会话 ID
     * @return 是否已合并
     */
    public Mono<Boolean> isMerged(String sessionId) {
        return Mono.fromCallable(() -> {
            Path mergedPath = Paths.get(storagePath, sessionId, "merged.wav");
            return Files.exists(mergedPath);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 获取合并进度
     *
     * @param sessionId 会话 ID
     * @return 进度百分比（0-100）
     */
    public Mono<Integer> getMergeProgress(String sessionId) {
        return Mono.justOrEmpty(mergeProgressMap.get(sessionId));
    }

    /**
     * 执行录音合并
     */
    private RecordingInfo doMergeRecordings(String sessionId) {
        mergeProgressMap.put(sessionId, 0);
        // 获取所有片段（使用 MyBatis-Plus 条件查询）
        LambdaQueryWrapper<InterviewRecording> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewRecording::getSessionId, sessionId)
                .orderByAsc(InterviewRecording::getSegmentIndex);
        List<InterviewRecording> segments = recordingMapper.selectList(wrapper);
        if (segments.isEmpty()) {
            log.warn("[RecordingMerge] 未找到片段, sessionId={}", sessionId);
            return RecordingInfo.builder()
                    .sessionId(sessionId)
                    .totalDurationMs(0)
                    .build();
        }
        mergeProgressMap.put(sessionId, 20);
        try {
            // 合并音频
            byte[] mergedAudio = mergeAudioFiles(segments);
            mergeProgressMap.put(sessionId, 60);
            // 保存合并后的音频文件
            Path mergedPath = saveMergedAudio(sessionId, mergedAudio);
            mergeProgressMap.put(sessionId, 80);
            // 生成文字记录
            List<RecordingInfo.TranscriptEntry> transcript = generateTranscript(segments);
            // 计算总时长
            int totalDurationMs = segments.stream()
                    .mapToInt(s -> s.getDurationMs() != null ? s.getDurationMs() : 0)
                    .sum();
            // 保存或更新索引记录
            saveOrUpdateIndex(sessionId, totalDurationMs, mergedPath.toString(), transcript);
            mergeProgressMap.put(sessionId, 100);
            log.info("[RecordingMerge] 合并完成, sessionId={}, duration={}ms, path={}",
                    sessionId, totalDurationMs, mergedPath);
            return RecordingInfo.builder()
                    .sessionId(sessionId)
                    .totalDurationMs(totalDurationMs)
                    .mergedAudioUrl(String.format("/landit/recordings/%s/merged.wav", sessionId))
                    .segments(segments.stream().map(this::convertToSegment).toList())
                    .transcript(transcript)
                    .build();
        } catch (IOException e) {
            log.error("[RecordingMerge] 合并录音失败, sessionId={}", sessionId, e);
            throw new RuntimeException("合并录音失败", e);
        }
    }

    /**
     * 分块流式返回音频数据
     */
    private Flux<byte[]> streamAudioInChunks(Path path) {
        try {
            byte[] audioData = Files.readAllBytes(path);
            // 分块返回，每块 8KB
            int chunkSize = 8192;
            int totalChunks = (audioData.length + chunkSize - 1) / chunkSize;
            return Flux.range(0, totalChunks)
                    .map(i -> {
                        int start = i * chunkSize;
                        int end = Math.min(start + chunkSize, audioData.length);
                        byte[] chunk = new byte[end - start];
                        System.arraycopy(audioData, start, chunk, 0, end - start);
                        return chunk;
                    });
        } catch (IOException e) {
            return Flux.error(e);
        }
    }

    /**
     * 合并音频文件
     */
    private byte[] mergeAudioFiles(List<InterviewRecording> segments) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (InterviewRecording segment : segments) {
            if (segment.getAudioPath() != null) {
                Path audioPath = Paths.get(segment.getAudioPath());
                if (Files.exists(audioPath)) {
                    byte[] audioData = Files.readAllBytes(audioPath);
                    outputStream.write(audioData);
                }
            }
        }
        byte[] pcmData = outputStream.toByteArray();
        return WavHeaderUtils.createWavWithHeader(pcmData);
    }

    /**
     * 保存合并后的音频文件
     */
    private Path saveMergedAudio(String sessionId, byte[] audioData) throws IOException {
        Path sessionDir = Paths.get(storagePath, sessionId);
        if (!Files.exists(sessionDir)) {
            Files.createDirectories(sessionDir);
        }
        Path mergedPath = sessionDir.resolve("merged.wav");
        Files.write(mergedPath, audioData);
        log.info("[RecordingMerge] 合并音频已保存, path={}, size={}", mergedPath, audioData.length);
        return mergedPath;
    }

    /**
     * 生成文字记录
     */
    private List<RecordingInfo.TranscriptEntry> generateTranscript(List<InterviewRecording> segments) {
        return segments.stream()
                .filter(s -> s.getContent() != null && !s.getContent().isEmpty())
                .map(s -> RecordingInfo.TranscriptEntry.builder()
                        .role(s.getRole())
                        .content(s.getContent())
                        .timestamp(convertToTimestamp(s.getStartTime()))
                        .segmentIndex(s.getSegmentIndex())
                        .build())
                .toList();
    }

    /**
     * 转换时间戳
     */
    private long convertToTimestamp(LocalDateTime startTime) {
        if (startTime == null) {
            return 0L;
        }
        return startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 保存或更新索引记录
     */
    private void saveOrUpdateIndex(String sessionId, int totalDurationMs, String mergedPath,
                                   List<RecordingInfo.TranscriptEntry> transcript) {
        try {
            String transcriptJson = objectMapper.writeValueAsString(transcript);
            // 使用 MyBatis-Plus 条件查询查找现有记录
            LambdaQueryWrapper<RecordingIndex> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RecordingIndex::getSessionId, sessionId);
            RecordingIndex existingIndex = indexMapper.selectOne(wrapper);
            if (existingIndex != null) {
                existingIndex.setTotalDurationMs(totalDurationMs);
                existingIndex.setMergedAudioPath(mergedPath);
                existingIndex.setTranscript(transcriptJson);
                indexMapper.updateById(existingIndex);
            } else {
                RecordingIndex index = new RecordingIndex();
                index.setSessionId(sessionId);
                index.setTotalDurationMs(totalDurationMs);
                index.setMergedAudioPath(mergedPath);
                index.setTranscript(transcriptJson);
                indexMapper.insert(index);
            }
        } catch (JsonProcessingException e) {
            log.error("[RecordingMerge] 序列化转录记录失败", e);
        }
    }

    /**
     * 转换为片段 DTO
     */
    private RecordingInfo.RecordingSegment convertToSegment(InterviewRecording recording) {
        return RecordingInfo.RecordingSegment.builder()
                .index(recording.getSegmentIndex())
                .role(recording.getRole())
                .content(recording.getContent())
                .durationMs(recording.getDurationMs())
                .startTime(recording.getStartTime())
                .endTime(recording.getEndTime())
                .audioUrl(String.format("/landit/recordings/%s/segments/%d/audio",
                        recording.getSessionId(), recording.getSegmentIndex()))
                .build();
    }
}
