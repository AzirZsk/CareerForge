package com.landit.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.landit.common.enums.InterviewSource;
import com.landit.common.exception.BusinessException;
import com.landit.common.response.PageResponse;
import com.landit.interview.dto.interviewcenter.*;
import com.landit.interview.entity.Interview;
import com.landit.interview.entity.InterviewPreparation;
import com.landit.interview.entity.InterviewReviewNote;
import com.landit.interview.entity.InterviewRound;
import com.landit.interview.mapper.InterviewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 面试中心服务
 * 管理真实面试的生命周期
 *
 * @author Azir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewCenterService extends ServiceImpl<InterviewMapper, Interview> {

    private final InterviewRoundService roundService;
    private final InterviewPreparationService preparationService;
    private final InterviewReviewNoteService reviewNoteService;

    /**
     * 创建真实面试（含轮次）
     */
    @Transactional(rollbackFor = Exception.class)
    public InterviewDetailVO createInterview(CreateInterviewRequest request) {
        Interview interview = new Interview();
        interview.setSource(InterviewSource.REAL.getCode());
        interview.setStatus("preparing");
        interview.setCompany(request.getCompanyName());
        interview.setPosition(request.getPosition());
        interview.setDate(request.getInterviewDate());
        interview.setJdContent(request.getJdContent());
        interview.setNotes(request.getNotes());
        interview.setCompanyResearch("{}");
        interview.setJdAnalysis("{}");
        this.save(interview);
        if (request.getRounds() != null && !request.getRounds().isEmpty()) {
            roundService.batchCreateRounds(interview.getId(), request.getRounds());
        }
        log.info("创建真实面试成功: id={}, company={}", interview.getId(), interview.getCompany());
        return getInterviewDetail(interview.getId());
    }

    /**
     * 获取面试列表（支持类型和状态筛选）
     */
    public PageResponse<InterviewListItemVO> getInterviewList(String type, String status, Integer page, Integer size) {
        LambdaQueryWrapper<Interview> wrapper = new LambdaQueryWrapper<>();
        if (!"all".equals(type)) {
            wrapper.eq(Interview::getSource, type);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Interview::getStatus, status);
        }
        wrapper.orderByDesc(Interview::getDate);
        Page<Interview> pageParam = new Page<>(page, size);
        Page<Interview> result = this.page(pageParam, wrapper);
        List<InterviewListItemVO> voList = result.getRecords().stream()
                .map(this::convertToListItemVO)
                .collect(Collectors.toList());
        return PageResponse.of(voList, result.getTotal(), page, size);
    }

    /**
     * 获取面试详情（含轮次、准备事项、复盘笔记）
     */
    public InterviewDetailVO getInterviewDetail(String id) {
        Interview interview = this.getById(id);
        if (interview == null) {
            throw new BusinessException("面试不存在: " + id);
        }
        List<InterviewRound> rounds = roundService.getByInterviewId(id);
        List<InterviewPreparation> preparations = preparationService.getByInterviewId(id);
        InterviewReviewNote reviewNote = reviewNoteService.getManualNoteByInterviewId(id);
        return convertToDetailVO(interview, rounds, preparations, reviewNote);
    }

    /**
     * 更新面试信息
     */
    @Transactional(rollbackFor = Exception.class)
    public InterviewDetailVO updateInterview(String id, UpdateInterviewRequest request) {
        Interview interview = this.getById(id);
        if (interview == null) {
            throw new BusinessException("面试不存在: " + id);
        }
        if (request.getCompanyName() != null) {
            interview.setCompany(request.getCompanyName());
        }
        if (request.getPosition() != null && !request.getPosition().isBlank()) {
            interview.setPosition(request.getPosition());
        }
        if (request.getInterviewDate() != null && !request.getInterviewDate().toString().isBlank()) {
            interview.setDate(request.getInterviewDate());
        }
        if (request.getJdContent() != null) {
            interview.setJdContent(request.getJdContent());
        }
        if (request.getNotes() != null) {
            interview.setNotes(request.getNotes());
        }
        this.updateById(interview);
        log.info("更新面试成功: id={}", id);
        return getInterviewDetail(id);
    }

    /**
     * 删除面试（级联删除轮次、准备事项、复盘笔记）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteInterview(String id) {
        Interview interview = this.getById(id);
        if (interview == null) {
            throw new BusinessException("面试不存在: " + id);
        }
        reviewNoteService.deleteByInterviewId(id);
        preparationService.deleteByInterviewId(id);
        roundService.deleteByInterviewId(id);
        this.removeById(id);
        log.info("删除面试成功: id={}", id);
    }

    /**
     * 重新计算面试状态（轮次状态变更后调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public void recalculateInterviewStatus(String interviewId) {
        List<InterviewRound> rounds = roundService.getByInterviewId(interviewId);
        String newStatus = calculateOverallStatus(rounds);
        String newResult = calculateOverallResult(rounds);
        Interview interview = this.getById(interviewId);
        if (interview != null) {
            interview.setStatus(newStatus);
            interview.setOverallResult(newResult);
            this.updateById(interview);
            log.info("重新计算面试状态: interviewId={}, status={}, result={}", interviewId, newStatus, newResult);
        }
    }

    private String calculateOverallStatus(List<InterviewRound> rounds) {
        if (rounds.isEmpty()) {
            return "preparing";
        }
        boolean hasPending = rounds.stream()
                .anyMatch(r -> "pending".equals(r.getStatus()) || "in_progress".equals(r.getStatus()));
        if (hasPending) {
            return "in_progress";
        }
        boolean allTerminal = rounds.stream()
                .allMatch(r -> "passed".equals(r.getStatus()) || "failed".equals(r.getStatus())
                        || "cancelled".equals(r.getStatus()));
        if (allTerminal) {
            return "completed";
        }
        return "in_progress";
    }

    private String calculateOverallResult(List<InterviewRound> rounds) {
        if (rounds.isEmpty()) {
            return null;
        }
        boolean hasFailed = rounds.stream().anyMatch(r -> "failed".equals(r.getStatus()));
        if (hasFailed) {
            return "failed";
        }
        boolean allPassed = rounds.stream().allMatch(r -> "passed".equals(r.getStatus()));
        if (allPassed) {
            return "passed";
        }
        boolean hasPendingResult = rounds.stream().anyMatch(r -> "pending_result".equals(r.getStatus()));
        if (hasPendingResult) {
            return "pending";
        }
        return null;
    }

    private InterviewListItemVO convertToListItemVO(Interview interview) {
        InterviewListItemVO vo = new InterviewListItemVO();
        BeanUtils.copyProperties(interview, vo);
        vo.setCompanyName(interview.getCompany());
        vo.setInterviewDate(interview.getDate());
        List<InterviewRound> rounds = roundService.getByInterviewId(interview.getId());
        vo.setRoundCount(rounds.size());
        vo.setCompletedRounds((int) rounds.stream().filter(r -> "passed".equals(r.getStatus())).count());
        return vo;
    }

    private InterviewDetailVO convertToDetailVO(Interview interview, List<InterviewRound> rounds,
                                                 List<InterviewPreparation> preparations,
                                                 InterviewReviewNote reviewNote) {
        InterviewDetailVO vo = new InterviewDetailVO();
        BeanUtils.copyProperties(interview, vo);
        vo.setCompanyName(interview.getCompany());
        vo.setInterviewDate(interview.getDate());
        vo.setRounds(rounds.stream().map(this::convertToRoundVO).collect(Collectors.toList()));
        vo.setPreparations(preparations.stream().map(this::convertToPreparationVO).collect(Collectors.toList()));
        if (reviewNote != null) {
            vo.setReviewNote(convertToReviewNoteVO(reviewNote));
        }
        return vo;
    }

    private RoundVO convertToRoundVO(InterviewRound round) {
        RoundVO vo = new RoundVO();
        BeanUtils.copyProperties(round, vo);
        return vo;
    }

    private PreparationVO convertToPreparationVO(InterviewPreparation preparation) {
        PreparationVO vo = new PreparationVO();
        BeanUtils.copyProperties(preparation, vo);
        return vo;
    }

    private ReviewNoteVO convertToReviewNoteVO(InterviewReviewNote note) {
        ReviewNoteVO vo = new ReviewNoteVO();
        BeanUtils.copyProperties(note, vo);
        return vo;
    }

}
