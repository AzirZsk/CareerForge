package com.careerforge.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careerforge.interview.entity.InterviewSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 面试会话Mapper接口
 *
 * @author Azir
 */
@Mapper
public interface InterviewSessionMapper extends BaseMapper<InterviewSession> {

}
