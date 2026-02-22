package com.landit.interview.handler;

import com.landit.common.enums.InterviewType;
import com.landit.common.service.AIService;
import com.landit.interview.dto.FinishSessionRequest;
import com.landit.interview.dto.FinishSessionResponse;
import com.landit.interview.dto.HintResponse;
import com.landit.interview.dto.StartSessionRequest;
import com.landit.interview.dto.StartSessionResponse;
import com.landit.interview.dto.SubmitAnswerRequest;
import com.landit.interview.dto.SubmitAnswerResponse;
import com.landit.interview.service.InterviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 面试业务编排处理器
 * 负责处理涉及会话管理、AI评分、提示生成等聚合操作
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewHandler {

    private final InterviewService interviewService;
    private final AIService aiService;

    /**
     * 开始面试会话
     * 涉及：创建会话记录 -> 选择面试题目
     *
     * @param request 开始会话请求
     * @return 会话响应
     */
    @Transactional(rollbackFor = Exception.class)
    public StartSessionResponse startInterviewSession(StartSessionRequest request) {
        // TODO: 实现开始面试会话逻辑
        // 1. 创建面试会话记录
        // 2. 根据类型选择面试题目
        // 3. 返回第一道题目
        log.info("开始面试会话: type={}", request.getType());
        return null;
    }

    /**
     * 提交回答
     * 涉及：保存回答 -> AI评分
     *
     * @param sessionId 会话ID
     * @param request   提交回答请求
     * @return 评分响应
     */
    @Transactional(rollbackFor = Exception.class)
    public SubmitAnswerResponse submitAnswer(String sessionId, SubmitAnswerRequest request) {
        // TODO: 实现提交回答逻辑
        // 1. 保存用户回答
        // 2. 调用 AI 进行评分
        // 3. 返回评分结果
        log.info("提交回答: sessionId={}, questionId={}", sessionId, request.getQuestionId());
        return null;
    }

    /**
     * 请求提示
     * 涉及：AI 生成提示
     *
     * @param sessionId  会话ID
     * @param questionId 题目ID
     * @return 提示响应
     */
    public HintResponse getHint(String sessionId, String questionId) {
        // TODO: 实现获取提示逻辑
        // 1. 获取当前题目
        // 2. 调用 AI 生成提示
        log.info("请求提示: sessionId={}, questionId={}", sessionId, questionId);
        return null;
    }

    /**
     * 结束面试
     * 涉及：更新会话状态 -> 生成复盘记录
     *
     * @param sessionId 会话ID
     * @param request   结束请求
     * @return 结束响应
     */
    @Transactional(rollbackFor = Exception.class)
    public FinishSessionResponse finishInterview(String sessionId, FinishSessionRequest request) {
        // TODO: 实现结束面试逻辑
        // 1. 更新会话状态为已完成
        // 2. 计算总分
        // 3. 生成复盘记录
        log.info("结束面试: sessionId={}", sessionId);
        return null;
    }

}
