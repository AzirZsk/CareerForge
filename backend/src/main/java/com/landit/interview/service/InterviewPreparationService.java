package com.landit.interview.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.landit.interview.dto.interviewcenter.*;
import com.landit.interview.entity.InterviewPreparation;

import java.util.List;

/**
 * 面试准备事项服务接口
 *
 * @author Azir
 */
public interface InterviewPreparationService extends IService<InterviewPreparation> {

    /**
     * 获取面试的所有准备事项
     */
    List<InterviewPreparation> getByInterviewId(String interviewId);

    /**
     * 添加准备事项
     */
    PreparationVO addPreparation(String interviewId, AddPreparationRequest request);

    /**
     * 更新准备事项
     */
    PreparationVO updatePreparation(String interviewId, String preparationId, UpdatePreparationRequest request);

    /**
     * 切换准备事项完成状态
     */
    PreparationVO toggleComplete(String interviewId, String preparationId);

    /**
     * 删除准备事项
     */
    void deletePreparation(String interviewId, String preparationId);

    /**
     * 根据面试ID删除所有准备事项
     */
    void deleteByInterviewId(String interviewId);

}
