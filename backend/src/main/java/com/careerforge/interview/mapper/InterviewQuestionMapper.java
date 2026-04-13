package com.careerforge.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careerforge.interview.entity.InterviewQuestion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 面试题目Mapper接口
 *
 * @author Azir
 */
@Mapper
public interface InterviewQuestionMapper extends BaseMapper<InterviewQuestion> {

}
