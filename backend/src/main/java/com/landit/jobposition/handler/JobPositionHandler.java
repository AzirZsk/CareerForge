package com.landit.jobposition.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.landit.common.exception.BusinessException;
import com.landit.common.response.PageResponse;
import com.landit.company.entity.Company;
import com.landit.company.service.CompanyService;
import com.landit.interview.entity.Interview;
import com.landit.interview.service.InterviewService;
import com.landit.jobposition.dto.*;
import com.landit.jobposition.entity.JobPosition;
import com.landit.jobposition.service.JobPositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 职位 Handler
 * 处理职位相关的业务逻辑
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobPositionHandler {

    private final JobPositionService jobPositionService;
    private final CompanyService companyService;
    private final InterviewService interviewService;

    /**
     * 获取职位列表（含面试统计）
     *
     * @param page 页码
     * @param size 每页大小
     * @return 职位列表
     */
    public PageResponse<JobPositionListItemVO> getJobPositionList(Integer page, Integer size) {
        log.info("获取职位列表: page={}, size={}", page, size);
        Page<JobPosition> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<JobPosition> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(JobPosition::getUpdatedAt);
        Page<JobPosition> result = jobPositionService.page(pageParam, wrapper);
        List<JobPositionListItemVO> voList = result.getRecords().stream()
                .map(this::convertToListItemVO)
                .collect(Collectors.toList());
        return PageResponse.of(voList, result.getTotal(), page, size);
    }

    /**
     * 创建职位
     *
     * @param request 创建请求
     * @return 职位详情
     */
    @Transactional(rollbackFor = Exception.class)
    public JobPositionDetailVO createJobPosition(CreateJobPositionRequest request) {
        log.info("创建职位: companyName={}, title={}", request.getCompanyName(), request.getTitle());
        // 查找或创建公司
        Company company = companyService.findByName(request.getCompanyName())
                .orElseGet(() -> {
                    Company newCompany = new Company();
                    newCompany.setName(request.getCompanyName());
                    companyService.save(newCompany);
                    return newCompany;
                });
        // 检查职位是否已存在
        Optional<JobPosition> existing = jobPositionService.findByCompanyIdAndTitle(
                String.valueOf(company.getId()), request.getTitle());
        if (existing.isPresent()) {
            throw new BusinessException("该公司下已存在同名职位: " + request.getTitle());
        }
        // 创建职位
        JobPosition jobPosition = new JobPosition();
        jobPosition.setCompanyId(String.valueOf(company.getId()));
        jobPosition.setTitle(request.getTitle());
        jobPosition.setJdContent(request.getJdContent());
        jobPositionService.save(jobPosition);
        return convertToDetailVO(jobPosition);
    }

    /**
     * 获取职位详情
     *
     * @param id 职位ID
     * @return 职位详情
     */
    public JobPositionDetailVO getJobPositionDetail(String id) {
        log.info("获取职位详情: id={}", id);
        JobPosition jobPosition = jobPositionService.getById(id);
        if (jobPosition == null) {
            throw new BusinessException("职位不存在: " + id);
        }
        return convertToDetailVO(jobPosition);
    }

    /**
     * 更新职位
     *
     * @param id      职位ID
     * @param request 更新请求
     * @return 职位详情
     */
    @Transactional(rollbackFor = Exception.class)
    public JobPositionDetailVO updateJobPosition(String id, UpdateJobPositionRequest request) {
        log.info("更新职位: id={}", id);
        JobPosition jobPosition = jobPositionService.getById(id);
        if (jobPosition == null) {
            throw new BusinessException("职位不存在: " + id);
        }
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            jobPosition.setTitle(request.getTitle());
        }
        if (request.getJdContent() != null) {
            jobPosition.setJdContent(request.getJdContent());
        }
        jobPositionService.updateById(jobPosition);
        return convertToDetailVO(jobPosition);
    }

    /**
     * 删除职位
     *
     * @param id 职位ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteJobPosition(String id) {
        log.info("删除职位: id={}", id);
        JobPosition jobPosition = jobPositionService.getById(id);
        if (jobPosition == null) {
            throw new BusinessException("职位不存在: " + id);
        }
        // 检查是否有关联面试
        LambdaQueryWrapper<Interview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Interview::getJobPositionId, id);
        long count = interviewService.count(wrapper);
        if (count > 0) {
            throw new BusinessException("该职位下存在 " + count + " 个关联面试，无法删除");
        }
        jobPositionService.removeById(id);
    }

    /**
     * 获取职位下的面试列表
     *
     * @param id 职位ID
     * @return 面试简要列表
     */
    public List<JobPositionDetailVO.InterviewBriefVO> getJobPositionInterviews(String id) {
        log.info("获取职位面试列表: id={}", id);
        LambdaQueryWrapper<Interview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Interview::getJobPositionId, id)
                .orderByDesc(Interview::getDate);
        List<Interview> interviews = interviewService.list(wrapper);
        return interviews.stream()
                .map(this::convertToInterviewBriefVO)
                .collect(Collectors.toList());
    }

    // ===== 以下为原有方法，保持兼容 =====

    /**
     * 根据公司ID和职位名称获取职位信息
     *
     * @param companyId 公司ID
     * @param title     职位名称
     * @return 职位 VO
     */
    public JobPositionVO getJobPosition(String companyId, String title) {
        return jobPositionService.findByCompanyIdAndTitle(companyId, title)
                .map(jobPositionService::toVO)
                .orElse(null);
    }

    /**
     * 检查职位是否存在且分析有效
     *
     * @param companyId 公司ID
     * @param title     职位名称
     * @return true 如果存在且有效
     */
    public boolean hasValidAnalysis(String companyId, String title) {
        JobPosition jobPosition = jobPositionService.findByCompanyIdAndTitle(companyId, title).orElse(null);
        return jobPositionService.hasValidAnalysis(jobPosition);
    }

    /**
     * 保存JD分析结果
     *
     * @param companyId  公司ID
     * @param title      职位名称
     * @param jdContent  JD原文
     * @param jdAnalysis JD分析结果（JSON格式）
     * @return 职位实体
     */
    @Transactional(rollbackFor = Exception.class)
    public JobPosition saveAnalysis(String companyId, String title, String jdContent, String jdAnalysis) {
        Optional<JobPosition> existingJobPosition = jobPositionService.findByCompanyIdAndTitle(companyId, title);
        JobPosition jobPosition;
        if (existingJobPosition.isPresent()) {
            jobPosition = existingJobPosition.get();
            jobPosition.setJdContent(jdContent);
            jobPosition.setJdAnalysis(jdAnalysis);
            jobPosition.setJdAnalysisUpdatedAt(LocalDateTime.now());
            jobPositionService.updateById(jobPosition);
        } else {
            jobPosition = new JobPosition();
            jobPosition.setCompanyId(companyId);
            jobPosition.setTitle(title);
            jobPosition.setJdContent(jdContent);
            jobPosition.setJdAnalysis(jdAnalysis);
            jobPosition.setJdAnalysisUpdatedAt(LocalDateTime.now());
            jobPositionService.save(jobPosition);
        }
        return jobPosition;
    }

    // ===== 私有转换方法 =====

    /**
     * 转换为列表项 VO
     */
    private JobPositionListItemVO convertToListItemVO(JobPosition jobPosition) {
        Company company = companyService.getById(jobPosition.getCompanyId());
        // 查询关联面试（按日期降序，取最新面试时间）
        LambdaQueryWrapper<Interview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Interview::getJobPositionId, jobPosition.getId())
                .orderByDesc(Interview::getDate);
        List<Interview> interviews = interviewService.list(wrapper);
        int interviewCount = interviews.size();
        LocalDateTime latestInterviewDate = interviews.isEmpty() ? null : interviews.get(0).getDate();
        return JobPositionListItemVO.builder()
                .id(String.valueOf(jobPosition.getId()))
                .companyName(company != null ? company.getName() : "未知公司")
                .title(jobPosition.getTitle())
                .interviewCount(interviewCount)
                .latestInterviewDate(latestInterviewDate)
                .createdAt(jobPosition.getCreatedAt())
                .updatedAt(jobPosition.getUpdatedAt())
                .build();
    }

    /**
     * 转换为详情 VO
     */
    private JobPositionDetailVO convertToDetailVO(JobPosition jobPosition) {
        Company company = companyService.getById(jobPosition.getCompanyId());
        // 获取关联面试
        List<JobPositionDetailVO.InterviewBriefVO> interviews = getJobPositionInterviews(String.valueOf(jobPosition.getId()));
        return JobPositionDetailVO.builder()
                .id(String.valueOf(jobPosition.getId()))
                .companyId(jobPosition.getCompanyId())
                .companyName(company != null ? company.getName() : "未知公司")
                .title(jobPosition.getTitle())
                .jdContent(jobPosition.getJdContent())
                .jdAnalysis(jobPosition.getJdAnalysis())
                .jdAnalysisUpdatedAt(jobPosition.getJdAnalysisUpdatedAt())
                .createdAt(jobPosition.getCreatedAt())
                .updatedAt(jobPosition.getUpdatedAt())
                .interviews(interviews)
                .build();
    }

    /**
     * 转换为面试简要 VO
     */
    private JobPositionDetailVO.InterviewBriefVO convertToInterviewBriefVO(Interview interview) {
        return JobPositionDetailVO.InterviewBriefVO.builder()
                .id(String.valueOf(interview.getId()))
                .status(interview.getStatus())
                .date(interview.getDate())
                .overallResult(interview.getOverallResult())
                .build();
    }

}
