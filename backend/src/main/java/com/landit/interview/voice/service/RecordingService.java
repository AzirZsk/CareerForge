package com.landit.interview.voice.service;

import com.landit.interview.voice.dto.RecordingInfo;
import com.landit.interview.voice.dto.RecordingSegment;
import com.landit.interview.voice.entity.InterviewRecording;

import java.util.List;

/**
 * 录音服务接口
 * 处理面试录音的存储和管理
 *
 * @author Azir
 */
public interface RecordingService {

    /**
     * 保存录音片段
     *
     * @param sessionId  会话 ID
     * @param segment    录音片段数据
     */
    void saveSegment(String sessionId, RecordingSegment segment);

    /**
     * 保存录音片段（实体）
     *
     * @param recording 录音实体
     */
    void saveRecording(InterviewRecording recording);

    /**
     * 获取录音片段列表
     *
     * @param sessionId 会话 ID
     * @return 录音片段列表
     */
    List<InterviewRecording> getSegments(String sessionId);

    /**
     * 获取录音回放信息
     *
     * @param sessionId 会话 ID
     * @return 录音信息
     */
    RecordingInfo getRecordingInfo(String sessionId);

    /**
     * 获取录音文件路径
     *
     * @param sessionId 会话 ID
     * @return 录音文件路径
     */
    String getRecordingPath(String sessionId);

    /**
     * 删除会话的所有录音
     *
     * @param sessionId 会话 ID
     */
    void deleteRecordings(String sessionId);
}
