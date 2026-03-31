package com.landit.interview.handler;

import com.landit.interview.dto.interviewcenter.*;
import com.landit.interview.service.InterviewRoundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 面试轮次业务编排处理器
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewRoundHandler {

    private final InterviewRoundService interviewRoundService;

    /**
     * 新增面试轮次
     */
    @Transactional(rollbackFor = Exception.class)
    public RoundVO addRound(String interviewId, AddRoundRequest request) {
        log.info("新增轮次: interviewId={}, type={}", interviewId, request.getRoundType());
        return interviewRoundService.addRound(interviewId, request);
    }

    /**
     * 更新轮次信息
     */
    @Transactional(rollbackFor = Exception.class)
    public RoundVO updateRound(String interviewId, String roundId, UpdateRoundRequest request) {
        log.info("更新轮次: roundId={}", roundId);
        return interviewRoundService.updateRound(interviewId, roundId, request);
    }

    /**
     * 更新轮次状态（含状态流转校验）
     */
    @Transactional(rollbackFor = Exception.class)
    public RoundVO updateRoundStatus(String interviewId, String roundId, UpdateRoundStatusRequest request) {
        log.info("更新轮次状态: roundId={}, status={}", roundId, request.getStatus());
        return interviewRoundService.updateRoundStatus(interviewId, roundId, request.getStatus());
    }

    /**
     * 删除轮次
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRound(String interviewId, String roundId) {
        log.info("删除轮次: roundId={}", roundId);
        interviewRoundService.deleteRound(interviewId, roundId);
    }

    /**
     * 调整轮次顺序
     */
    @Transactional(rollbackFor = Exception.class)
    public List<RoundVO> reorderRounds(String interviewId, ReorderRoundsRequest request) {
        log.info("调整轮次顺序: interviewId={}", interviewId);
        return interviewRoundService.reorderRounds(interviewId, request.getRoundIds());
    }

}
