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
import com.landit.interview.service.InterviewCenterService;
import com.landit.interview.service.InterviewPreparationService;
import com.landit.interview.service.InterviewReviewNoteService;
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
        interview.setRoundType(request.getRoundType());
        interview.setRoundName(request.getRoundName());
        interviewCenterService.save(interview);
        log.info("创建真实面试成功: id={}, company={}, jobPositionId={}, roundType={}",
                interview.getId(), interview.getCompany(), interview.getJobPositionId(), request.getRoundType());
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
        List<InterviewPreparation> preparations = preparationService.getByInterviewId(id);
        InterviewReviewNote reviewNote = reviewNoteService.getManualNoteByInterviewId(id);
        return convertToDetailVO(interview, preparations, reviewNote);
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
        if (request.getRoundType() != null && !request.getRoundType().isBlank()) {
            interview.setRoundType(request.getRoundType());
        }
        if (request.getRoundName() != null) {
            interview.setRoundName(request.getRoundName());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            interview.setStatus(request.getStatus());
        }
        if (request.getOverallResult() != null) {
            interview.setOverallResult(request.getOverallResult());
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
        interviewCenterService.removeById(id);
        log.info("删除面试成功: id={}", id);
    }

    // ===== 私有业务逻辑方法 =====

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
        vo.setRoundType(interview.getRoundType());
        return vo;
    }

    private InterviewDetailVO convertToDetailVO(Interview interview,
                                                 List<InterviewPreparation> preparations,
                                                 InterviewReviewNote reviewNote) {
        InterviewDetailVO vo = new InterviewDetailVO();
        BeanUtils.copyProperties(interview, vo);
        vo.setCompanyName(interview.getCompany());
        vo.setInterviewDate(interview.getDate());
        vo.setRoundType(interview.getRoundType());
        vo.setRoundName(interview.getRoundName());
        vo.setPreparations(preparations.stream().map(this::convertToPreparationVO).collect(Collectors.toList()));
        if (reviewNote != null) {
            vo.setReviewNote(convertToReviewNoteVO(reviewNote));
        }
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
