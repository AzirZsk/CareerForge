package com.landit.interview.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.landit.common.enums.InterviewType;
import com.landit.common.response.PageResponse;
import com.landit.interview.dto.FinishSessionRequest;
import com.landit.interview.dto.FinishSessionResponse;
import com.landit.interview.dto.HintResponse;
import com.landit.interview.dto.InterviewDetailVO;
import com.landit.interview.dto.InterviewQuestionsVO;
import com.landit.interview.dto.StartSessionRequest;
import com.landit.interview.dto.StartSessionResponse;
import com.landit.interview.dto.SubmitAnswerRequest;
import com.landit.interview.dto.SubmitAnswerResponse;
import com.landit.interview.entity.Interview;
import com.landit.interview.mapper.InterviewMapper;
import org.springframework.stereotype.Service;

/**
 * 面试服务实现类
 *
 * @author Azir
 */
@Service
public class InterviewService extends ServiceImpl<InterviewMapper, Interview> {

    /**
     * 开始面试会话
     */
    public StartSessionResponse startInterviewSession(StartSessionRequest request) {
        return null;
    }

    /**
     * 提交回答
     */
    public SubmitAnswerResponse submitAnswer(Long sessionId, SubmitAnswerRequest request) {
        return null;
    }

    /**
     * 请求提示
     */
    public HintResponse getHint(Long sessionId, Long questionId) {
        return null;
    }

    /**
     * 结束面试
     */
    public FinishSessionResponse finishInterview(Long sessionId, FinishSessionRequest request) {
        return null;
    }

    /**
     * 获取面试历史
     */
    public PageResponse<Interview> getInterviewHistory(InterviewType type, Integer page, Integer size) {
        return null;
    }

    /**
     * 获取面试详情
     */
    public InterviewDetailVO getInterviewDetail(Long id) {
        return null;
    }

    /**
     * 获取题库
     */
    public InterviewQuestionsVO getInterviewQuestions(InterviewType type) {
        return null;
    }

}
