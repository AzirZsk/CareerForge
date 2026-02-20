package com.landit.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.landit.interview.entity.Interview;
import org.apache.ibatis.annotations.Mapper;

/**
 * 面试记录Mapper接口
 *
 * @author Azir
 */
@Mapper
public interface InterviewMapper extends BaseMapper<Interview> {

}
