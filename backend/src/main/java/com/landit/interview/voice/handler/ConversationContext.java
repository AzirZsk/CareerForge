package com.landit.interview.voice.handler;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 语音面试对话上下文
 * 存储面试过程中的会话状态、对话历史和音频数据
 *
 * @author Azir
 */
@Slf4j
@Data
public class ConversationContext {
    // 面试上下文（从真实面试加载）
    /**
     * 面试岗位名称
     */
    private String position;
    /**
     * 职位描述（JD）内容
     */
    private String jdContent;
    /**
     * 简历内容
     */
    private String resumeContent;
    /**
     * 面试开始时间（用于计算已面试时长）
     */
    private LocalDateTime interviewStartTime = LocalDateTime.now();
    /**
     * 对话历史记录
     */
    private StringBuilder conversationHistory = new StringBuilder();
    /**
     * 录音片段索引（原子递增）
     */
    private AtomicInteger segmentIndex = new AtomicInteger(0);
    /**
     * 候选人音频缓冲区
     */
    private ByteArrayOutputStream candidateAudioBuffer = new ByteArrayOutputStream();
    /**
     * 录音片段开始时间
     */
    private LocalDateTime segmentStartTime;

    /**
     * 添加候选人消息到对话历史
     *
     * @param message 候选人消息
     */
    public void addCandidateMessage(String message) {
        conversationHistory.append("候选人：").append(message).append("\n");
    }

    /**
     * 添加面试官消息到对话历史
     *
     * @param message 面试官消息
     */
    public void addInterviewerMessage(String message) {
        conversationHistory.append("面试官：").append(message).append("\n");
    }

    /**
     * 获取对话摘要（最近 2000 字符）
     *
     * @return 对话摘要
     */
    public String getConversationSummary() {
        if (conversationHistory.length() > 2000) {
            return conversationHistory.substring(conversationHistory.length() - 2000);
        }
        return conversationHistory.toString();
    }

    /**
     * 获取已面试时长（秒）
     *
     * @return 已面试时长
     */
    public long getElapsedSeconds() {
        return Duration.between(interviewStartTime, LocalDateTime.now()).getSeconds();
    }

    /**
     * 获取下一个片段索引（原子递增）
     *
     * @return 片段索引
     */
    public int getNextSegmentIndex() {
        return segmentIndex.getAndIncrement();
    }

    /**
     * 追加候选人音频数据到缓冲区
     *
     * @param audio 音频数据
     */
    public void appendCandidateAudio(byte[] audio) {
        try {
            candidateAudioBuffer.write(audio);
        } catch (Exception e) {
            log.error("Failed to append candidate audio", e);
        }
    }

    /**
     * 获取候选人音频数据并重置缓冲区
     *
     * @return 音频数据
     */
    public byte[] getCandidateAudioAndReset() {
        byte[] audio = candidateAudioBuffer.toByteArray();
        candidateAudioBuffer.reset();
        return audio;
    }

    /**
     * 开始新的录音片段
     */
    public void startSegment() {
        segmentStartTime = LocalDateTime.now();
    }

    /**
     * 获取片段开始时间
     *
     * @return 片段开始时间
     */
    public LocalDateTime getSegmentStartTime() {
        return segmentStartTime;
    }
}
