package com.landit.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.landit.job.entity.Job;
import org.apache.ibatis.annotations.Mapper;

/**
 * 职位Mapper接口
 *
 * @author Azir
 */
@Mapper
public interface JobMapper extends BaseMapper<Job> {

}
