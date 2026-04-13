package com.careerforge.interview.voice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.careerforge.interview.voice.dto.RecordingInfo;
import com.careerforge.interview.voice.dto.RecordingSegment;
import com.careerforge.interview.voice.entity.InterviewRecording;
import com.careerforge.interview.voice.mapper.InterviewRecordingMapper;
import com.careerforge.interview.voice.util.WavHeaderUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 录音服务
 * 处理面试录音的存储和管理
 *
 * @author Azir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecordingService extends ServiceImpl<InterviewRecordingMapper, InterviewRecording> {

    @Value("${careerforge.recording.storage-path:./data/recordings}")
    private String storagePath;

    /**
     * 保存录音片段
     */
    public void saveSegment(String sessionId, RecordingSegment segment) {
        log.debug("[Recording] 保存片段, sessionId={}, index={}", sessionId, segment.getIndex());
        InterviewRecording recording = convertToEntity(sessionId, segment);
        if (segment.getAudioData() != null && segment.getAudioData().length > 0) {
            String audioPath = saveAudioFile(sessionId, segment.getIndex(), segment.getAudioData());
            recording.setAudioPath(audioPath);
        }
        this.save(recording);
    }

    /**
     * 保存录音片段（实体）
     */
    public void saveRecording(InterviewRecording recording) {
        log.debug("[Recording] 保存录音, sessionId={}, index={}",
                recording.getSessionId(), recording.getSegmentIndex());
        this.save(recording);
    }

    /**
     * 获取录音片段列表
     */
    public List<InterviewRecording> getSegments(String sessionId) {
        log.debug("[Recording] 获取片段列表, sessionId={}", sessionId);
        LambdaQueryWrapper<InterviewRecording> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewRecording::getSessionId, sessionId)
                .orderByAsc(InterviewRecording::getSegmentIndex);
        return this.list(wrapper);
    }

    /**
     * 获取录音回放信息
     */
    public RecordingInfo getRecordingInfo(String sessionId) {
        log.debug("[Recording] 获取录音信息, sessionId={}", sessionId);
        List<InterviewRecording> recordings = getSegments(sessionId);
        if (recordings.isEmpty()) {
            return RecordingInfo.builder()
                    .sessionId(sessionId)
                    .totalDurationMs(0)
                    .build();
        }
        int totalDurationMs = recordings.stream()
                .mapToInt(r -> r.getDurationMs() != null ? r.getDurationMs() : 0)
                .sum();
        List<RecordingInfo.RecordingSegment> segments = recordings.stream()
                .map(this::convertToInfoSegment)
                .collect(Collectors.toList());
        List<RecordingInfo.TranscriptEntry> transcript = recordings.stream()
                .filter(r -> r.getContent() != null && !r.getContent().isEmpty())
                .map(this::convertToTranscriptEntry)
                .collect(Collectors.toList());
        String mergedAudioUrl = getMergedAudioUrl(sessionId);
        return RecordingInfo.builder()
                .sessionId(sessionId)
                .totalDurationMs(totalDurationMs)
                .mergedAudioUrl(mergedAudioUrl)
                .segments(segments)
                .transcript(transcript)
                .build();
    }

    /**
     * 获取录音文件路径
     */
    public String getRecordingPath(String sessionId) {
        Path sessionPath = Paths.get(storagePath, sessionId);
        if (!Files.exists(sessionPath)) {
            return null;
        }
        Path mergedPath = sessionPath.resolve("merged.wav");
        if (Files.exists(mergedPath)) {
            return mergedPath.toString();
        }
        return sessionPath.toString();
    }

    /**
     * 删除会话的所有录音
     */
    public void deleteRecordings(String sessionId) {
        log.info("[Recording] 删除录音, sessionId={}", sessionId);
        LambdaQueryWrapper<InterviewRecording> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewRecording::getSessionId, sessionId);
        this.remove(wrapper);
        Path sessionPath = Paths.get(storagePath, sessionId);
        if (Files.exists(sessionPath)) {
            try {
                deleteDirectory(sessionPath.toFile());
                log.info("[Recording] 已删除录音文件, path={}", sessionPath);
            } catch (IOException e) {
                log.error("[Recording] 删除录音文件失败, path={}", sessionPath, e);
            }
        }
    }

    /**
     * 保存音频文件到磁盘
     */
    private String saveAudioFile(String sessionId, Integer segmentIndex, byte[] audioData) {
        try {
            Path sessionDir = Paths.get(storagePath, sessionId);
            if (!Files.exists(sessionDir)) {
                Files.createDirectories(sessionDir);
            }
            String fileName = String.format("segment_%04d.pcm", segmentIndex);
            Path filePath = sessionDir.resolve(fileName);
            Files.write(filePath, audioData);
            log.debug("[Recording] 音频文件已保存, path={}, size={}", filePath, audioData.length);
            return filePath.toString();
        } catch (IOException e) {
            log.error("[Recording] 保存音频文件失败, sessionId={}, index={}", sessionId, segmentIndex, e);
            return null;
        }
    }

    /**
     * 获取合并后的音频 URL
     */
    private String getMergedAudioUrl(String sessionId) {
        Path mergedPath = Paths.get(storagePath, sessionId, "merged.wav");
        if (Files.exists(mergedPath)) {
            return String.format("/recordings/%s/merged.wav", sessionId);
        }
        return null;
    }

    /**
     * 转换为实体
     */
    private InterviewRecording convertToEntity(String sessionId, RecordingSegment segment) {
        InterviewRecording recording = new InterviewRecording();
        recording.setSessionId(sessionId);
        recording.setSegmentIndex(segment.getIndex());
        recording.setRole(segment.getRole());
        recording.setContent(segment.getContent());
        recording.setDurationMs(segment.getDurationMs());
        recording.setStartTime(segment.getStartTime());
        recording.setEndTime(segment.getEndTime());
        return recording;
    }

    /**
     * 转换为 RecordingInfo 中的片段 DTO
     */
    private RecordingInfo.RecordingSegment convertToInfoSegment(InterviewRecording recording) {
        return RecordingInfo.RecordingSegment.builder()
                .index(recording.getSegmentIndex())
                .role(recording.getRole())
                .content(recording.getContent())
                .durationMs(recording.getDurationMs())
                .startTime(recording.getStartTime())
                .endTime(recording.getEndTime())
                .audioUrl(String.format("/recordings/%s/segments/%d/audio",
                        recording.getSessionId(), recording.getSegmentIndex()))
                .build();
    }

    /**
     * 转换为文字记录条目
     */
    private RecordingInfo.TranscriptEntry convertToTranscriptEntry(InterviewRecording recording) {
        return RecordingInfo.TranscriptEntry.builder()
                .role(recording.getRole())
                .content(recording.getContent())
                .timestamp(WavHeaderUtils.convertToTimestamp(recording.getStartTime()))
                .segmentIndex(recording.getSegmentIndex())
                .build();
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(File directory) throws IOException {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        if (!directory.delete()) {
            throw new IOException("Failed to delete: " + directory);
        }
    }

}
