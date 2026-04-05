package com.landit.interview.voice.gateway;

import com.landit.interview.voice.dto.VoiceRequest;
import com.landit.interview.voice.dto.VoiceResponse;
import com.landit.interview.voice.dto.VoiceSessionCreateRequest;
import com.landit.interview.voice.dto.VoiceSessionCreateVO;
import org.springframework.web.socket.WebSocketSession;

/**
 * 语音面试网关接口
 * 负责会话状态管理、角色路由、录音存储
 * 使用 Spring 原生 WebSocket API
 *
 * @author Azir
 */
public interface InterviewVoiceGateway {

    /**
     * 创建语音面试会话
     *
     * @param request 创建请求
     * @return 会话信息
     */
    VoiceSessionCreateVO createSession(VoiceSessionCreateRequest request);

    /**
     * 注册 WebSocket 会话
     *
     * @param sessionId 面试会话 ID
     * @param wsSession WebSocket 会话
     */
    void registerSession(String sessionId, WebSocketSession wsSession);

    /**
     * 注销 WebSocket 会话
     *
     * @param sessionId 面试会话 ID
     * @param wsSession WebSocket 会话
     */
    void unregisterSession(String sessionId, WebSocketSession wsSession);

    /**
     * 处理请求
     *
     * @param sessionId 面试会话 ID
     * @param wsSession WebSocket 会话
     * @param request   请求
     */
    void handleRequest(String sessionId, WebSocketSession wsSession, VoiceRequest request);

    /**
     * 处理音频数据
     *
     * @param sessionId 面试会话 ID
     * @param wsSession WebSocket 会话
     * @param audioData 音频数据
     */
    void handleAudioData(String sessionId, WebSocketSession wsSession, byte[] audioData);

    /**
     * 处理会话错误
     *
     * @param sessionId 面试会话 ID
     * @param wsSession WebSocket 会话
     * @param error     错误
     */
    void handleSessionError(String sessionId, WebSocketSession wsSession, Throwable error);

    /**
     * 冻结面试
     *
     * @param sessionId 面试会话 ID
     */
    void freezeInterview(String sessionId);

    /**
     * 恢复面试
     *
     * @param sessionId 面试会话 ID
     */
    void resumeInterview(String sessionId);

    /**
     * 结束面试
     *
     * @param sessionId 面试会话 ID
     */
    void endInterview(String sessionId);

    /**
     * 获取会话状态
     *
     * @param sessionId 面试会话 ID
     * @return 状态数据
     */
    VoiceResponse.StateData getSessionState(String sessionId);

    /**
     * 发送响应
     *
     * @param sessionId 面试会话 ID
     * @param response  响应
     */
    void sendResponse(String sessionId, VoiceResponse response);

    /**
     * 广播响应给所有会话
     *
     * @param response 响应
     */
    void broadcastResponse(VoiceResponse response);
}
