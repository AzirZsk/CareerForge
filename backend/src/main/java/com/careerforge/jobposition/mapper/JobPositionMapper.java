package com.careerforge.jobposition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careerforge.jobposition.entity.JobPosition;
import org.apache.ibatis.annotations.Mapper;

/**
 * 职位 Mapper 接口
 *
 * @author Azir
 */
@Mapper
public interface JobPositionMapper extends BaseMapper<JobPosition> {

}
