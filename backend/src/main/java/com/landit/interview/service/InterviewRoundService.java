package com.landit.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.landit.common.enums.RoundStatus;
import com.landit.common.exception.BusinessException;
import com.landit.interview.dto.interviewcenter.*;
import com.landit.interview.entity.InterviewRound;
import com.landit.interview.mapper.InterviewRoundMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 面试轮次服务
 *
 * @author Azir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewRoundService extends ServiceImpl<InterviewRoundMapper, InterviewRound> {

    @Autowired
    @Lazy
    private InterviewCenterService interviewCenterService;

    /**
     * 获取面试的所有轮次（按顺序排列）
     */
    public List<InterviewRound> getByInterviewId(String interviewId) {
        LambdaQueryWrapper<InterviewRound> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewRound::getInterviewId, interviewId)
                .orderByAsc(InterviewRound::getRoundOrder);
        return this.list(wrapper);
    }

    /**
     * 批量创建轮次
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchCreateRounds(String interviewId, List<AddRoundRequest> requests) {
        int order = getNextRoundOrder(interviewId);
        for (AddRoundRequest request : requests) {
            InterviewRound round = new InterviewRound();
            round.setInterviewId(interviewId);
            round.setRoundType(request.getRoundType());
            round.setRoundName(request.getRoundName());
            round.setRoundOrder(order++);
            round.setStatus(RoundStatus.PENDING.getCode());
            round.setScheduledDate(request.getScheduledDate());
            this.save(round);
        }
        interviewCenterService.recalculateInterviewStatus(interviewId);
        log.info("批量创建轮次成功: interviewId={}, count={}", interviewId, requests.size());
    }

    /**
     * 添加轮次
     */
    @Transactional(rollbackFor = Exception.class)
    public RoundVO addRound(String interviewId, AddRoundRequest request) {
        InterviewRound round = new InterviewRound();
        round.setInterviewId(interviewId);
        round.setRoundType(request.getRoundType());
        round.setRoundName(request.getRoundName());
        round.setRoundOrder(getNextRoundOrder(interviewId));
        round.setStatus(RoundStatus.PENDING.getCode());
        round.setScheduledDate(request.getScheduledDate());
        this.save(round);
        interviewCenterService.recalculateInterviewStatus(interviewId);
        log.info("添加轮次成功: interviewId={}, roundId={}", interviewId, round.getId());
        return convertToVO(round);
    }

    /**
     * 更新轮次信息
     */
    @Transactional(rollbackFor = Exception.class)
    public RoundVO updateRound(String interviewId, String roundId, UpdateRoundRequest request) {
        InterviewRound round = this.getById(roundId);
        if (round == null || !round.getInterviewId().equals(interviewId)) {
            throw new BusinessException("轮次不存在或不属于该面试");
        }
        if (request.getRoundType() != null) {
            round.setRoundType(request.getRoundType());
        }
        if (request.getRoundName() != null) {
            round.setRoundName(request.getRoundName());
        }
        if (request.getScheduledDate() != null) {
            round.setScheduledDate(request.getScheduledDate());
        }
        if (request.getActualDate() != null) {
            round.setActualDate(request.getActualDate());
        }
        if (request.getNotes() != null) {
            round.setNotes(request.getNotes());
        }
        if (request.getSelfRating() != null) {
            round.setSelfRating(request.getSelfRating());
        }
        if (request.getResultNote() != null) {
            round.setResultNote(request.getResultNote());
        }
        this.updateById(round);
        log.info("更新轮次成功: roundId={}", roundId);
        return convertToVO(round);
    }

    /**
     * 更新轮次状态（含状态流转校验）
     */
    @Transactional(rollbackFor = Exception.class)
    public RoundVO updateRoundStatus(String interviewId, String roundId, String status) {
        InterviewRound round = this.getById(roundId);
        if (round == null || !round.getInterviewId().equals(interviewId)) {
            throw new BusinessException("轮次不存在或不属于该面试");
        }
        RoundStatus currentStatus = RoundStatus.fromCode(round.getStatus());
        RoundStatus targetStatus = RoundStatus.fromCode(status);
        if (currentStatus == null || targetStatus == null) {
            throw new BusinessException("无效的状态值");
        }
        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new BusinessException("状态流转不合法: " + currentStatus.getCode() + " -> " + targetStatus.getCode());
        }
        round.setStatus(targetStatus.getCode());
        if (targetStatus == RoundStatus.IN_PROGRESS) {
            round.setActualDate(java.time.LocalDateTime.now());
        }
        this.updateById(round);
        interviewCenterService.recalculateInterviewStatus(interviewId);
        log.info("更新轮次状态成功: roundId={}, status={}", roundId, status);
        return convertToVO(round);
    }

    /**
     * 删除轮次
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRound(String interviewId, String roundId) {
        InterviewRound round = this.getById(roundId);
        if (round == null || !round.getInterviewId().equals(interviewId)) {
            throw new BusinessException("轮次不存在或不属于该面试");
        }
        RoundStatus status = RoundStatus.fromCode(round.getStatus());
        if (status != null && status != RoundStatus.PENDING && status != RoundStatus.CANCELLED) {
            throw new BusinessException("只能删除待面试或已取消的轮次");
        }
        this.removeById(roundId);
        reorderAfterDelete(interviewId);
        interviewCenterService.recalculateInterviewStatus(interviewId);
        log.info("删除轮次成功: roundId={}", roundId);
    }

    /**
     * 重排轮次顺序
     */
    @Transactional(rollbackFor = Exception.class)
    public List<RoundVO> reorderRounds(String interviewId, List<String> roundIds) {
        List<InterviewRound> rounds = getByInterviewId(interviewId);
        if (rounds.size() != roundIds.size()) {
            throw new BusinessException("轮次数量不匹配");
        }
        Map<String, InterviewRound> roundMap = rounds.stream()
                .collect(Collectors.toMap(InterviewRound::getId, Function.identity()));
        for (int i = 0; i < roundIds.size(); i++) {
            String roundId = roundIds.get(i);
            InterviewRound round = roundMap.get(roundId);
            if (round == null) {
                throw new BusinessException("轮次不存在: " + roundId);
            }
            round.setRoundOrder(i + 1);
            this.updateById(round);
        }
        log.info("重排轮次顺序成功: interviewId={}", interviewId);
        return getByInterviewId(interviewId).stream().map(this::convertToVO).collect(Collectors.toList());
    }

    /**
     * 根据面试ID删除所有轮次
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteByInterviewId(String interviewId) {
        LambdaQueryWrapper<InterviewRound> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewRound::getInterviewId, interviewId);
        this.remove(wrapper);
        log.info("删除面试所有轮次: interviewId={}", interviewId);
    }

    /**
     * 获取下一个轮次顺序号
     */
    public int getNextRoundOrder(String interviewId) {
        List<InterviewRound> rounds = getByInterviewId(interviewId);
        return rounds.size() + 1;
    }

    private void reorderAfterDelete(String interviewId) {
        List<InterviewRound> rounds = getByInterviewId(interviewId);
        for (int i = 0; i < rounds.size(); i++) {
            InterviewRound round = rounds.get(i);
            round.setRoundOrder(i + 1);
            this.updateById(round);
        }
    }

    private RoundVO convertToVO(InterviewRound round) {
        RoundVO vo = new RoundVO();
        BeanUtils.copyProperties(round, vo);
        return vo;
    }

}
