package com.landit.jobposition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.landit.jobposition.dto.JobPositionVO;
import com.landit.jobposition.entity.JobPosition;
import com.landit.jobposition.mapper.JobPositionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 职位服务类
 * 负责职位表的 CRUD 操作
 *
 * @author Azir
 */
@Service
@RequiredArgsConstructor
public class JobPositionService extends ServiceImpl<JobPositionMapper, JobPosition> {

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

}
