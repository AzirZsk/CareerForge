package com.landit.interview.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.landit.interview.dto.interviewcenter.*;
import com.landit.interview.entity.InterviewRound;

import java.util.List;

/**
 * 面试轮次服务接口
 *
 * @author Azir
 */
public interface InterviewRoundService extends IService<InterviewRound> {

    /**
     * 获取面试的所有轮次（按顺序排列）
     */
    List<InterviewRound> getByInterviewId(String interviewId);

    /**
     * 批量创建轮次
     */
    void batchCreateRounds(String interviewId, List<AddRoundRequest> requests);

    /**
     * 添加轮次
     */
    RoundVO addRound(String interviewId, AddRoundRequest request);

    /**
     * 更新轮次信息
     */
    RoundVO updateRound(String interviewId, String roundId, UpdateRoundRequest request);

    /**
     * 更新轮次状态（含状态流转校验）
     */
    RoundVO updateRoundStatus(String interviewId, String roundId, String status);

    /**
     * 删除轮次
     */
    void deleteRound(String interviewId, String roundId);

    /**
     * 重排轮次顺序
     */
    List<RoundVO> reorderRounds(String interviewId, List<String> roundIds);

    /**
     * 根据面试ID删除所有轮次
     */
    void deleteByInterviewId(String interviewId);

    /**
     * 获取下一个轮次顺序号
     */
    int getNextRoundOrder(String interviewId);

}
