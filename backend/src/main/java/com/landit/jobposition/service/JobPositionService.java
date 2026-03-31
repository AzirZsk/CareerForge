package com.landit.jobposition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.landit.common.exception.BusinessException;
import com.landit.company.entity.Company;
import com.landit.company.service.CompanyService;
import com.landit.interview.entity.Interview;
import com.landit.interview.service.InterviewService;
import com.landit.jobposition.dto.*;
import com.landit.jobposition.entity.JobPosition;
import com.landit.jobposition.mapper.JobPositionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 职位服务类
 * 负责职位表的 CRUD 操作
 *
 * @author Azir
 */
@Service
@RequiredArgsConstructor
public class JobPositionService extends ServiceImpl<JobPositionMapper, JobPosition> {

    private final CompanyService companyService;
    private final InterviewService interviewService;

    /**
     * 根据公司ID和职位名称查询职位
     *
     * @param companyId 公司ID
     * @param title     职位名称
     * @return 职位实体
     */
    public Optional<JobPosition> findByCompanyIdAndTitle(String companyId, String title) {
        LambdaQueryWrapper<JobPosition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobPosition::getCompanyId, companyId)
                .eq(JobPosition::getTitle, title);
        return Optional.ofNullable(getOne(wrapper));
    }

    /**
     * 创建或更新职位JD分析信息
     *
     * @param companyId   公司ID
     * @param title       职位名称
     * @param jdContent   JD原文
     * @param jdAnalysis  JD分析结果（JSON格式）
     * @return 职位实体
     */
    public JobPosition createOrUpdateJobPosition(String companyId, String title, String jdContent, String jdAnalysis) {
        Optional<JobPosition> existingJobPosition = findByCompanyIdAndTitle(companyId, title);
        JobPosition jobPosition;
        if (existingJobPosition.isPresent()) {
            jobPosition = existingJobPosition.get();
            jobPosition.setJdContent(jdContent);
            jobPosition.setJdAnalysis(jdAnalysis);
            jobPosition.setJdAnalysisUpdatedAt(LocalDateTime.now());
            updateById(jobPosition);
        } else {
            jobPosition = new JobPosition();
            jobPosition.setCompanyId(companyId);
            jobPosition.setTitle(title);
            jobPosition.setJdContent(jdContent);
            jobPosition.setJdAnalysis(jdAnalysis);
            jobPosition.setJdAnalysisUpdatedAt(LocalDateTime.now());
            save(jobPosition);
        }
        return jobPosition;
    }

    /**
     * 检查JD分析是否有效（存在且有分析结果）
     *
     * @param jobPosition 职位实体
     * @return true 如果有效
     */
    public boolean hasValidAnalysis(JobPosition jobPosition) {
        return jobPosition != null
                && jobPosition.getJdAnalysis() != null
                && !jobPosition.getJdAnalysis().isEmpty();
    }

    /**
     * 转换为 VO
     *
     * @param jobPosition 职位实体
     * @return JobPositionVO
     */
    public JobPositionVO toVO(JobPosition jobPosition) {
        if (jobPosition == null) {
            return null;
        }
        return JobPositionVO.builder()
                .id(String.valueOf(jobPosition.getId()))
                .companyId(jobPosition.getCompanyId())
                .title(jobPosition.getTitle())
                .jdContent(jobPosition.getJdContent())
                .jdAnalysis(jobPosition.getJdAnalysis())
                .jdAnalysisUpdatedAt(jobPosition.getJdAnalysisUpdatedAt())
                .build();
    }

    /**
     * 获取职位列表（含面试统计）
     *
     * @param page 页码
     * @param size 每页大小
     * @return 职位列表
     */
    public IPage<JobPositionListItemVO> getJobPositionList(Integer page, Integer size) {
        Page<JobPosition> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<JobPosition> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(JobPosition::getUpdatedAt);
        Page<JobPosition> result = this.page(pageParam, wrapper);

        return result.convert(this::convertToListItemVO);
    }

    /**
     * 创建职位
     *
     * @param request 创建请求
     * @return 职位详情
     */
    @Transactional(rollbackFor = Exception.class)
    public JobPositionDetailVO createJobPosition(CreateJobPositionRequest request) {
        // 查找或创建公司
        Company company = companyService.findByName(request.getCompanyName())
                .orElseGet(() -> {
                    Company newCompany = new Company();
                    newCompany.setName(request.getCompanyName());
                    companyService.save(newCompany);
                    return newCompany;
                });

        // 检查职位是否已存在
        Optional<JobPosition> existing = findByCompanyIdAndTitle(String.valueOf(company.getId()), request.getTitle());
        if (existing.isPresent()) {
            throw new BusinessException("该公司下已存在同名职位: " + request.getTitle());
        }

        // 创建职位
        JobPosition jobPosition = new JobPosition();
        jobPosition.setCompanyId(String.valueOf(company.getId()));
        jobPosition.setTitle(request.getTitle());
        jobPosition.setJdContent(request.getJdContent());
        this.save(jobPosition);

        return convertToDetailVO(jobPosition);
    }

    /**
     * 获取职位详情
     *
     * @param id 职位ID
     * @return 职位详情
     */
    public JobPositionDetailVO getJobPositionDetail(String id) {
        JobPosition jobPosition = this.getById(id);
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
        JobPosition jobPosition = this.getById(id);
        if (jobPosition == null) {
            throw new BusinessException("职位不存在: " + id);
        }
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            jobPosition.setTitle(request.getTitle());
        }
        if (request.getJdContent() != null) {
            jobPosition.setJdContent(request.getJdContent());
        }
        this.updateById(jobPosition);
        return convertToDetailVO(jobPosition);
    }

    /**
     * 删除职位
     *
     * @param id 职位ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteJobPosition(String id) {
        JobPosition jobPosition = this.getById(id);
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
        this.removeById(id);
    }

    /**
     * 获取职位下的面试列表
     *
     * @param jobPositionId 职位ID
     * @return 面试简要列表
     */
    public List<JobPositionDetailVO.InterviewBriefVO> getJobPositionInterviews(String jobPositionId) {
        LambdaQueryWrapper<Interview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Interview::getJobPositionId, jobPositionId)
                .orderByDesc(Interview::getDate);
        List<Interview> interviews = interviewService.list(wrapper);
        return interviews.stream()
                .map(this::convertToInterviewBriefVO)
                .collect(Collectors.toList());
    }

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
