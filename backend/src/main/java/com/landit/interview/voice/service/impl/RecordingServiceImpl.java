package com.landit.interview.voice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.landit.interview.voice.dto.RecordingInfo;
import com.landit.interview.voice.dto.RecordingSegment;
import com.landit.interview.voice.entity.InterviewRecording;
import com.landit.interview.voice.mapper.InterviewRecordingMapper;
import com.landit.interview.voice.service.RecordingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 录音服务实现
 * 处理面试录音的存储和管理
 *
 * @author Azir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecordingServiceImpl implements RecordingService {

    private final InterviewRecordingMapper recordingMapper;

    @Value("${landit.recording.storage-path:./data/recordings}")
    private String storagePath;

    @Override
    public void saveSegment(String sessionId, RecordingSegment segment) {
        log.debug("[Recording] Saving segment, sessionId={}, index={}", sessionId, segment.getIndex());
        InterviewRecording recording = convertToEntity(sessionId, segment);
        if (segment.getAudioData() != null && segment.getAudioData().length > 0) {
            String audioPath = saveAudioFile(sessionId, segment.getIndex(), segment.getAudioData());
            recording.setAudioPath(audioPath);
        }
        recordingMapper.insert(recording);
    }

    @Override
    public void saveRecording(InterviewRecording recording) {
        log.debug("[Recording] Saving recording, sessionId={}, index={}",
                recording.getSessionId(), recording.getSegmentIndex());
        recordingMapper.insert(recording);
    }

    @Override
    public List<InterviewRecording> getSegments(String sessionId) {
        log.debug("[Recording] Getting segments, sessionId={}", sessionId);
        LambdaQueryWrapper<InterviewRecording> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewRecording::getSessionId, sessionId)
                .orderByAsc(InterviewRecording::getSegmentIndex);
        return recordingMapper.selectList(wrapper);
    }

    @Override
    public RecordingInfo getRecordingInfo(String sessionId) {
        log.debug("[Recording] Getting recording info, sessionId={}", sessionId);
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
        List<RecordingSegment> segments = recordings.stream()
                .map(this::convertToSegment)
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

    @Override
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

    @Override
    public void deleteRecordings(String sessionId) {
        log.info("[Recording] Deleting recordings, sessionId={}", sessionId);
        LambdaQueryWrapper<InterviewRecording> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewRecording::getSessionId, sessionId);
        recordingMapper.delete(wrapper);
        Path sessionPath = Paths.get(storagePath, sessionId);
        if (Files.exists(sessionPath)) {
            try {
                deleteDirectory(sessionPath.toFile());
                log.info("[Recording] Deleted recording files, path={}", sessionPath);
            } catch (IOException e) {
                log.error("[Recording] Failed to delete recording files, path={}", sessionPath, e);
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
            log.debug("[Recording] Saved audio file, path={}, size={}", filePath, audioData.length);
            return filePath.toString();
        } catch (IOException e) {
            log.error("[Recording] Failed to save audio file, sessionId={}, index={}", sessionId, segmentIndex, e);
            return null;
        }
    }

    /**
     * 获取合并后的音频 URL
     */
    private String getMergedAudioUrl(String sessionId) {
        Path mergedPath = Paths.get(storagePath, sessionId, "merged.wav");
        if (Files.exists(mergedPath)) {
            return String.format("/landit/recordings/%s/merged.wav", sessionId);
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
     * 转换为 DTO
     */
    private RecordingSegment convertToSegment(InterviewRecording recording) {
        return RecordingSegment.builder()
                .index(recording.getSegmentIndex())
                .role(recording.getRole())
                .content(recording.getContent())
                .durationMs(recording.getDurationMs())
                .startTime(recording.getStartTime())
                .endTime(recording.getEndTime())
                .build();
    }

    /**
     * 转换为文字记录条目
     */
    private RecordingInfo.TranscriptEntry convertToTranscriptEntry(InterviewRecording recording) {
        return RecordingInfo.TranscriptEntry.builder()
                .role(recording.getRole())
                .content(recording.getContent())
                .timestamp(recording.getStartTime() != null ?
                        recording.getStartTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 0L)
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
