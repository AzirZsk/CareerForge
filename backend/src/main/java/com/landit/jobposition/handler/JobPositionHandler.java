package com.landit.jobposition.handler;

import com.landit.jobposition.dto.JobPositionVO;
import com.landit.jobposition.entity.JobPosition;
import com.landit.jobposition.service.JobPositionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 职位 Handler
 * 处理职位相关的业务逻辑
 *
 * @author Azir
 */
@Component
@RequiredArgsConstructor
public class JobPositionHandler {

    private final JobPositionService jobPositionService;

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
