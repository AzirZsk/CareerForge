package com.landit.job.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.landit.common.response.PageResponse;
import com.landit.job.entity.Job;
import com.landit.job.mapper.JobMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 职位服务实现类
 *
 * @author Azir
 */
@Service
@RequiredArgsConstructor
public class JobService extends ServiceImpl<JobMapper, Job> {

    /**
     * 获取推荐职位
     */
    public PageResponse<Job> getJobRecommendations(Integer page, Integer size) {
        return null;
    }

}
