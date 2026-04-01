package com.landit.interview.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.landit.common.enums.InterviewSource;
import com.landit.common.exception.BusinessException;
import com.landit.common.response.PageResponse;
import com.landit.company.entity.Company;
import com.landit.company.service.CompanyService;
import com.landit.interview.dto.interviewcenter.*;
import com.landit.interview.entity.Interview;
import com.landit.interview.entity.InterviewPreparation;
import com.landit.interview.entity.InterviewReviewNote;
import com.landit.interview.entity.InterviewRound;
import com.landit.interview.service.InterviewCenterService;
import com.landit.interview.service.InterviewPreparationService;
import com.landit.interview.service.InterviewReviewNoteService;
import com.landit.interview.service.InterviewRoundService;
import com.landit.jobposition.entity.JobPosition;
import com.landit.jobposition.service.JobPositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final InterviewRoundService roundService;
    private final InterviewPreparationService preparationService;
    private final InterviewReviewNoteService reviewNoteService;
    private final JobPositionService jobPositionService;
    private final CompanyService companyService;

    /**
     * 创建真实面试
     */
    @Transactional(rollbackFor = Exception.class)
    public InterviewDetailVO createInterview(CreateInterviewRequest request) {
        log.info("创建真实面试: company={}, position={}", request.getCompanyName(), request.getPosition());
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
        interviewCenterService.save(interview);
        if (request.getRounds() != null && !request.getRounds().isEmpty()) {
            roundService.batchCreateRounds(interview.getId(), request.getRounds());
        }
        log.info("创建真实面试成功: id={}, company={}, jobPositionId={}",
                interview.getId(), interview.getCompany(), interview.getJobPositionId());
        return getInterviewDetail(interview.getId());
    }

    /**
     * 获取面试列表
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
        Page<Interview> result = interviewCenterService.page(pageParam, wrapper);
        List<InterviewListItemVO> voList = result.getRecords().stream()
                .map(this::convertToListItemVO)
                .collect(Collectors.toList());
        return PageResponse.of(voList, result.getTotal(), page, size);
    }

    /**
     * 获取面试详情
     */
    public InterviewDetailVO getInterviewDetail(String id) {
        Interview interview = interviewCenterService.getById(id);
        if (interview == null) {
            throw new BusinessException("面试不存在: " + id);
        }
        List<InterviewRound> rounds = roundService.getByInterviewId(id);
        List<InterviewPreparation> preparations = preparationService.getByInterviewId(id);
        InterviewReviewNote reviewNote = reviewNoteService.getManualNoteByInterviewId(id);
        return convertToDetailVO(interview, rounds, preparations, reviewNote);
    }

    /**
     * 更新面试基本信息
     */
    @Transactional(rollbackFor = Exception.class)
    public InterviewDetailVO updateInterview(String id, UpdateInterviewRequest request) {
        log.info("更新面试: id={}", id);
        Interview interview = interviewCenterService.getById(id);
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
        interviewCenterService.updateById(interview);
        log.info("更新面试成功: id={}", id);
        return getInterviewDetail(id);
    }

    /**
     * 删除面试
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteInterview(String id) {
        log.info("删除面试: id={}", id);
        Interview interview = interviewCenterService.getById(id);
        if (interview == null) {
            throw new BusinessException("面试不存在: " + id);
        }
        reviewNoteService.deleteByInterviewId(id);
        preparationService.deleteByInterviewId(id);
        roundService.deleteByInterviewId(id);
        interviewCenterService.removeById(id);
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
        Interview interview = interviewCenterService.getById(interviewId);
        if (interview != null) {
            interview.setStatus(newStatus);
            interview.setOverallResult(newResult);
            interviewCenterService.updateById(interview);
            log.info("重新计算面试状态: interviewId={}, status={}, result={}", interviewId, newStatus, newResult);
        }
    }

    // ===== 私有业务逻辑方法 =====

    /**
     * 计算整体面试状态
     */
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

    /**
     * 计算整体面试结果
     */
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

    /**
     * 查找或创建职位记录
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

    // ===== 转换方法 =====

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
