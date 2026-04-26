package com.careerforge.interview.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.careerforge.interview.entity.Interview;
import com.careerforge.interview.mapper.InterviewMapper;
import org.springframework.stereotype.Service;

/**
 * 面试服务实现类，提供面试表的基础 CRUD 操作
 *
 * @author Azir
 */
@Service
public class InterviewService extends ServiceImpl<InterviewMapper, Interview> {

}
