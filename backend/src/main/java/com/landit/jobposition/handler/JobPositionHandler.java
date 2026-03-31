package com.landit.jobposition.handler;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.landit.common.response.PageResponse;
import com.landit.jobposition.dto.*;
import com.landit.jobposition.entity.JobPosition;
import com.landit.jobposition.service.JobPositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

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

    /**
     * 获取职位列表（含面试统计）
     *
     * @param page 页码
     * @param size 每页大小
     * @return 职位列表
     */
    public PageResponse<JobPositionListItemVO> getJobPositionList(Integer page, Integer size) {
        log.info("获取职位列表: page={}, size={}", page, size);
        IPage<JobPositionListItemVO> result = jobPositionService.getJobPositionList(page, size);
        return PageResponse.of(
                result.getRecords(),
                result.getTotal(),
                (int) result.getCurrent(),
                (int) result.getSize()
        );
    }

    /**
     * 创建职位
     *
     * @param request 创建请求
     * @return 职位详情
     */
    public JobPositionDetailVO createJobPosition(CreateJobPositionRequest request) {
        log.info("创建职位: companyName={}, title={}", request.getCompanyName(), request.getTitle());
        return jobPositionService.createJobPosition(request);
    }

    /**
     * 获取职位详情
     *
     * @param id 职位ID
     * @return 职位详情
     */
    public JobPositionDetailVO getJobPositionDetail(String id) {
        log.info("获取职位详情: id={}", id);
        return jobPositionService.getJobPositionDetail(id);
    }

    /**
     * 更新职位
     *
     * @param id      职位ID
     * @param request 更新请求
     * @return 职位详情
     */
    public JobPositionDetailVO updateJobPosition(String id, UpdateJobPositionRequest request) {
        log.info("更新职位: id={}", id);
        return jobPositionService.updateJobPosition(id, request);
    }

    /**
     * 删除职位
     *
     * @param id 职位ID
     */
    public void deleteJobPosition(String id) {
        log.info("删除职位: id={}", id);
        jobPositionService.deleteJobPosition(id);
    }

    /**
     * 获取职位下的面试列表
     *
     * @param id 职位ID
     * @return 面试简要列表
     */
    public List<JobPositionDetailVO.InterviewBriefVO> getJobPositionInterviews(String id) {
        log.info("获取职位面试列表: id={}", id);
        return jobPositionService.getJobPositionInterviews(id);
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
    public JobPosition saveAnalysis(String companyId, String title, String jdContent, String jdAnalysis) {
        return jobPositionService.createOrUpdateJobPosition(companyId, title, jdContent, jdAnalysis);
    }

}
