package com.landit.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.landit.interview.entity.InterviewSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 面试会话Mapper接口
 *
 * @author Azir
 */
@Mapper
public interface InterviewSessionMapper extends BaseMapper<InterviewSession> {

}
