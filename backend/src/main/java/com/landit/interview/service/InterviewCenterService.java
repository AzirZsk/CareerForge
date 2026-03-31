package com.landit.interview.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.landit.common.response.PageResponse;
import com.landit.interview.dto.interviewcenter.*;
import com.landit.interview.entity.Interview;

/**
 * 面试中心服务接口
 * 管理真实面试的生命周期
 *
 * @author Azir
 */
public interface InterviewCenterService extends IService<Interview> {

    /**
     * 创建真实面试（含轮次）
     */
    InterviewDetailVO createInterview(CreateInterviewRequest request);

    /**
     * 获取面试列表（支持类型和状态筛选）
     */
    PageResponse<InterviewListItemVO> getInterviewList(String type, String status, Integer page, Integer size);

    /**
     * 获取面试详情（含轮次、准备事项、复盘笔记）
     */
    InterviewDetailVO getInterviewDetail(String id);

    /**
     * 更新面试基本信息
     */
    InterviewDetailVO updateInterview(String id, UpdateInterviewRequest request);

    /**
     * 删除面试（级联删除轮次、准备事项、复盘笔记）
     */
    void deleteInterview(String id);

    /**
     * 根据轮次状态自动计算面试整体状态
     */
    void recalculateInterviewStatus(String interviewId);

}
