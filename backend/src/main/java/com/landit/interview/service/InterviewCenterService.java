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

import com.landit.company.entity.Company;
import com.landit.company.service.CompanyService;
import com.landit.jobposition.entity.JobPosition;
import com.landit.jobposition.service.JobPositionService;

import java.util.List;
import java.util.Optional;
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
    private final JobPositionService jobPositionService;
    private final CompanyService companyService;

    /**
     * 创建真实面试（含轮次）
     */
    @Transactional(rollbackFor = Exception.class)
    public InterviewDetailVO createInterview(CreateInterviewRequest request) {
        // 处理职位关联/创建
        String jobPositionId = request.getJobPositionId();
        if (jobPositionId == null || jobPositionId.isBlank()) {
            // 新建职位模式：查找或创建 JobPosition
            jobPositionId = findOrCreateJobPosition(request.getCompanyName(), request.getPosition(), request.getJdContent());
        }
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
        interview.setJobPositionId(jobPositionId);
        this.save(interview);
        if (request.getRounds() != null && !request.getRounds().isEmpty()) {
            roundService.batchCreateRounds(interview.getId(), request.getRounds());
        }
        log.info("创建真实面试成功: id={}, company={}, jobPositionId={}",
                interview.getId(), interview.getCompany(), interview.getJobPositionId());
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
        if (request.getInterviewDate() != null) {
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

    /**
     * 查找或创建职位记录
     * 当用户选择"新建职位"模式创建面试时，需要确保对应的 JobPosition 记录存在
     *
     * @param companyName   公司名称
     * @param positionTitle 职位名称
     * @param jdContent     JD内容
     * @return 职位ID
     */
    private String findOrCreateJobPosition(String companyName, String positionTitle, String jdContent) {
        // 1. 查找或创建公司
        Company company = companyService.findByName(companyName)
                .orElseGet(() -> {
                    Company newCompany = new Company();
                    newCompany.setName(companyName);
                    companyService.save(newCompany);
                    return newCompany;
                });
        // 2. 查找是否已存在该职位
        Optional<JobPosition> existing = jobPositionService.findByCompanyIdAndTitle(
                String.valueOf(company.getId()), positionTitle);
        if (existing.isPresent()) {
            log.info("复用已有职位: id={}, company={}, title={}", existing.get().getId(), companyName, positionTitle);
            return String.valueOf(existing.get().getId());
        }
        // 3. 创建新职位
        JobPosition newPosition = new JobPosition();
        newPosition.setCompanyId(String.valueOf(company.getId()));
        newPosition.setTitle(positionTitle);
        newPosition.setJdContent(jdContent);
        jobPositionService.save(newPosition);
        log.info("创建新职位: id={}, company={}, title={}", newPosition.getId(), companyName, positionTitle);
        return String.valueOf(newPosition.getId());
    }

}
