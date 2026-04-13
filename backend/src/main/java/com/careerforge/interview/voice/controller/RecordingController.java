package com.careerforge.interview.voice.controller;

import com.careerforge.common.response.ApiResponse;
import com.careerforge.interview.voice.dto.RecordingInfo;
import com.careerforge.interview.voice.service.RecordingMergeService;
import com.careerforge.interview.voice.service.RecordingService;
import com.careerforge.interview.voice.util.WavHeaderUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 录音回放控制器
 * 处理面试录音的合并、回放和下载
 *
 * @author Azir
 */
@Slf4j
@RestController
@RequestMapping("/recordings")
@RequiredArgsConstructor
public class RecordingController {

    private final RecordingService recordingService;
    private final RecordingMergeService recordingMergeService;

    /**
     * 获取录音回放信息
     * 返回会话的所有录音片段和文字记录
     *
     * @param sessionId 面试会话 ID
     * @return 录音回放信息
     */
    @GetMapping("/{sessionId}")
    public Mono<ApiResponse<RecordingInfo>> getRecordingInfo(@PathVariable String sessionId) {
        log.info("[Recording] 获取录音信息, sessionId={}", sessionId);
        return recordingMergeService.mergeRecordings(sessionId)
                .map(ApiResponse::success)
                .onErrorResume(e -> {
                    log.error("[Recording] 获取录音信息失败, sessionId={}", sessionId, e);
                    return Mono.just(ApiResponse.serverError("获取录音信息失败: " + e.getMessage()));
                });
    }

    /**
     * 获取合并后的音频流
     * 返回完整的 WAV 格式音频
     *
     * @param sessionId 面试会话 ID
     * @return 音频数据流
     */
    @GetMapping(value = "/{sessionId}/audio", produces = "audio/wav")
    public ResponseEntity<Flux<byte[]>> streamAudio(@PathVariable String sessionId) {
        log.info("[Recording] 流式传输合并音频, sessionId={}", sessionId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/wav"));
        headers.set("Content-Disposition", String.format("attachment; filename=\"%s.wav\"", sessionId));
        Flux<byte[]> audioFlux = recordingMergeService.streamMergedAudio(sessionId)
                .onErrorResume(e -> {
                    log.error("[Recording] 流式传输音频失败, sessionId={}", sessionId, e);
                    return Flux.empty();
                });
        return new ResponseEntity<>(audioFlux, headers, HttpStatus.OK);
    }

    /**
     * 获取单个片段音频
     * 返回指定片段的 PCM 音频数据（添加 WAV 头）
     *
     * @param sessionId    面试会话 ID
     * @param segmentIndex 片段序号
     * @return 音频数据
     */
    @GetMapping("/{sessionId}/segments/{segmentIndex}/audio")
    public ResponseEntity<byte[]> getSegmentAudio(
            @PathVariable String sessionId,
            @PathVariable Integer segmentIndex) {
        log.info("[Recording] 获取片段音频, sessionId={}, index={}", sessionId, segmentIndex);
        String recordingPath = recordingService.getRecordingPath(sessionId);
        if (recordingPath == null) {
            log.warn("[Recording] 录音未找到, sessionId={}", sessionId);
            return ResponseEntity.notFound().build();
        }
        Path segmentPath = Paths.get(recordingPath, String.format("segment_%04d.pcm", segmentIndex));
        if (!Files.exists(segmentPath)) {
            log.warn("[Recording] 片段未找到, path={}", segmentPath);
            return ResponseEntity.notFound().build();
        }
        try {
            byte[] pcmData = Files.readAllBytes(segmentPath);
            byte[] wavData = WavHeaderUtils.createWavWithHeader(pcmData);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/wav"));
            headers.setContentLength(wavData.length);
            return new ResponseEntity<>(wavData, headers, HttpStatus.OK);
        } catch (IOException e) {
            log.error("[Recording] 读取片段失败, path={}", segmentPath, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 检查录音是否已合并
     *
     * @param sessionId 面试会话 ID
     * @return 是否已合并
     */
    @GetMapping("/{sessionId}/merged")
    public Mono<ApiResponse<Boolean>> isMerged(@PathVariable String sessionId) {
        return recordingMergeService.isMerged(sessionId)
                .map(ApiResponse::success);
    }

    /**
     * 获取合并进度
     *
     * @param sessionId 面试会话 ID
     * @return 进度百分比（0-100）
     */
    @GetMapping("/{sessionId}/progress")
    public Mono<ApiResponse<Integer>> getMergeProgress(@PathVariable String sessionId) {
        return recordingMergeService.getMergeProgress(sessionId)
                .map(ApiResponse::success)
                .defaultIfEmpty(ApiResponse.success(0));
    }
}
