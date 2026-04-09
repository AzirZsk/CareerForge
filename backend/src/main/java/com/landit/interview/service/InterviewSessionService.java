package com.landit.interview.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.landit.interview.entity.InterviewSession;
import com.landit.interview.mapper.InterviewSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 面试会话服务类
 *
 * @author Azir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewSessionService extends ServiceImpl<InterviewSessionMapper, InterviewSession> {

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
}
