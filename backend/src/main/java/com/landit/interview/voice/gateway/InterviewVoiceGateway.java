package com.landit.interview.voice.gateway;

import com.landit.interview.voice.dto.VoiceRequest;
import com.landit.interview.voice.dto.VoiceResponse;
import com.landit.interview.voice.dto.VoiceSessionCreateRequest;
import com.landit.interview.voice.dto.VoiceSessionCreateVO;
import jakarta.websocket.Session;

/**
 * 语音面试网关接口
 * 负责会话状态管理、角色路由、录音存储
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
    void registerSession(String sessionId, Session wsSession);

    /**
     * 注销 WebSocket 会话
     *
     * @param sessionId 面试会话 ID
     * @param wsSession WebSocket 会话
     */
    void unregisterSession(String sessionId, Session wsSession);

    /**
     * 处理请求消息
     *
     * @param sessionId 面试会话 ID
     * @param wsSession WebSocket 会话
     * @param request   语音请求
     */
    void handleRequest(String sessionId, Session wsSession, VoiceRequest request);

    /**
     * 处理音频数据
     *
     * @param sessionId 面试会话 ID
     * @param wsSession WebSocket 会话
     * @param audioData 音频数据（PCM）
     */
    void handleAudioData(String sessionId, Session wsSession, byte[] audioData);

    /**
     * 处理会话错误
     *
     * @param sessionId 面试会话 ID
     * @param wsSession WebSocket 会话
     * @param error     错误
     */
    void handleSessionError(String sessionId, Session wsSession, Throwable error);

    /**
     * 冻结面试（进入求助模式）
     *
     * @param sessionId 面试会话 ID
     */
    void freezeInterview(String sessionId);

    /**
     * 恢复面试（退出求助模式）
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
     * 发送响应给客户端
     *
     * @param sessionId 面试会话 ID
     * @param response  响应
     */
    void sendResponse(String sessionId, VoiceResponse response);

    /**
     * 广播响应给所有连接
     *
     * @param response 响应
     */
    void broadcastResponse(VoiceResponse response);
}
