package com.landit.interview.handler;

import com.landit.common.response.PageResponse;
import com.landit.interview.dto.interviewcenter.*;
import com.landit.interview.service.InterviewCenterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 面试中心业务编排处理器
 * 负责真实面试的业务逻辑编排
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewCenterHandler {

    private final InterviewCenterService interviewCenterService;

    /**
     * 创建真实面试
     */
    @Transactional(rollbackFor = Exception.class)
    public InterviewDetailVO createInterview(CreateInterviewRequest request) {
        log.info("创建真实面试: company={}, position={}", request.getCompanyName(), request.getPosition());
        return interviewCenterService.createInterview(request);
    }

    /**
     * 获取面试列表
     */
    public PageResponse<InterviewListItemVO> getInterviewList(String type, String status, Integer page, Integer size) {
        return interviewCenterService.getInterviewList(type, status, page, size);
    }

    /**
     * 获取面试详情
     */
    public InterviewDetailVO getInterviewDetail(String id) {
        return interviewCenterService.getInterviewDetail(id);
    }

    /**
     * 更新面试基本信息
     */
    @Transactional(rollbackFor = Exception.class)
    public InterviewDetailVO updateInterview(String id, UpdateInterviewRequest request) {
        log.info("更新面试: id={}", id);
        return interviewCenterService.updateInterview(id, request);
    }

    /**
     * 删除面试
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteInterview(String id) {
        log.info("删除面试: id={}", id);
        interviewCenterService.deleteInterview(id);
    }

}
