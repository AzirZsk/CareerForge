package com.landit.interview.voice.service;

import com.landit.interview.voice.dto.RecordingInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 录音合并服务接口
 * 处理面试录音的合并和管理
 *
 * @author Azir
 */
public interface RecordingMergeService {

    /**
     * 合并会话的所有录音片段
     * 将多个 PCM 片段合并为一个 WAV 文件
     *
     * @param sessionId 会话 ID
     * @return 合并后的录音信息
     */
    Mono<RecordingInfo> mergeRecordings(String sessionId);

    /**
     * 流式返回合并后的音频数据
     *
     * @param sessionId 会话 ID
     * @return 音频数据流
     */
    Flux<byte[]> streamMergedAudio(String sessionId);

    /**
     * 检查是否已合并
     *
     * @param sessionId 会话 ID
     * @return 是否已合并
     */
    Mono<Boolean> isMerged(String sessionId);

    /**
     * 获取合并进度
     *
     * @param sessionId 会话 ID
     * @return 进度百分比（0-100）
     */
    Mono<Integer> getMergeProgress(String sessionId);
}
