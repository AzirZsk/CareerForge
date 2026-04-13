package com.careerforge.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careerforge.interview.entity.Interview;
import org.apache.ibatis.annotations.Mapper;

/**
 * 面试记录Mapper接口
 *
 * @author Azir
 */
@Mapper
public interface InterviewMapper extends BaseMapper<Interview> {

}
