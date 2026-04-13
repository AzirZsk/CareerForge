package com.landit.interview.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.landit.common.response.PageResponse;
import com.landit.interview.dto.InterviewDetailVO;
import com.landit.interview.dto.InterviewQuestionsVO;
import com.landit.interview.entity.Interview;
import com.landit.interview.mapper.InterviewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 面试服务实现类
 * 仅负责面试表的 CRUD 操作，聚合操作由 InterviewHandler 处理
 *
 * @author Azir
 */
@Service
@RequiredArgsConstructor
public class InterviewService extends ServiceImpl<InterviewMapper, Interview> {

    /**
     * 获取面试历史
     */
    public PageResponse<Interview> getInterviewHistory(String type, Integer page, Integer size) {
        // TODO: 实现查询逻辑
        return null;
    }

    /**
     * 获取面试详情
     */
    public InterviewDetailVO getInterviewDetail(String id) {
        // TODO: 实现查询逻辑
        return null;
    }

    /**
     * 获取题库
     */
    public InterviewQuestionsVO getInterviewQuestions(String type) {
        // TODO: 实现查询逻辑
        return null;
    }

}
