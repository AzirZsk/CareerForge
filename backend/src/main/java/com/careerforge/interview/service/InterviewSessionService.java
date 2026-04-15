package com.careerforge.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.careerforge.interview.entity.Interview;
import com.careerforge.interview.entity.InterviewSession;
import com.careerforge.interview.mapper.InterviewSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 面试会话服务类
 *
 * @author Azir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewSessionService extends ServiceImpl<InterviewSessionMapper, InterviewSession> {

    private final InterviewService interviewService;

    /**
     * 更新预生成问题列表（JSON格式）
     *
     * @param sessionId        会话ID
     * @param questionsJson 问题列表 JSON 字符串
     */
    public void updatePreGeneratedQuestions(String sessionId, String questionsJson) {
        log.debug("[InterviewSessionService] 更新预生成问题, sessionId={}", sessionId);
        InterviewSession session = getById(sessionId);
        if (session != null) {
            session.setPreGeneratedQuestions(questionsJson);
            updateById(session);
            log.debug("[InterviewSessionService] 预生成问题已更新, sessionId={}", sessionId);
        } else {
            log.warn("[InterviewSessionService] 会话不存在, sessionId={}", sessionId);
        }
    }

    /**
     * 获取同一面试下最近的、带有预生成问题的会话
     *
     * @param interviewId 面试ID
     * @return 最近的会话（带预生成问题），不存在则返回 null
     */
    public InterviewSession getLatestSessionWithQuestions(String interviewId) {
        LambdaQueryWrapper<InterviewSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewSession::getInterviewId, interviewId)
                .isNotNull(InterviewSession::getPreGeneratedQuestions)
                .ne(InterviewSession::getPreGeneratedQuestions, "")
                .orderByDesc(InterviewSession::getCreatedAt)
                .last("LIMIT 1");
        return getOne(wrapper, false);
    }

    /**
     * 通过真实面试ID查找最近的带预生成问题的模拟面试会话
     * 查找路径：真实面试ID -> t_interview(parent_interview_id) -> t_interview_session(interview_id)
     *
     * @param realInterviewId 真实面试ID
     * @return 最近的模拟面试会话（带预生成问题），不存在则返回 null
     */
    public InterviewSession getLatestSessionByRealInterviewId(String realInterviewId) {
        // 查出所有以该真实面试为 parent 的 mock Interview
        LambdaQueryWrapper<Interview> interviewWrapper = new LambdaQueryWrapper<>();
        interviewWrapper.eq(Interview::getParentInterviewId, realInterviewId)
                .select(Interview::getId);
        List<Interview> mockInterviews = interviewService.list(interviewWrapper);
        if (mockInterviews.isEmpty()) {
            log.debug("[InterviewSessionService] 未找到关联的模拟面试记录, realInterviewId={}", realInterviewId);
            return null;
        }
        // 提取所有 mock Interview ID，查找最近的有预生成问题的 session
        List<String> mockInterviewIds = mockInterviews.stream()
                .map(interview -> String.valueOf(interview.getId()))
                .collect(Collectors.toList());
        LambdaQueryWrapper<InterviewSession> sessionWrapper = new LambdaQueryWrapper<>();
        sessionWrapper.in(InterviewSession::getInterviewId, mockInterviewIds)
                .isNotNull(InterviewSession::getPreGeneratedQuestions)
                .ne(InterviewSession::getPreGeneratedQuestions, "")
                .orderByDesc(InterviewSession::getCreatedAt)
                .last("LIMIT 1");
        return getOne(sessionWrapper, false);
    }
}
