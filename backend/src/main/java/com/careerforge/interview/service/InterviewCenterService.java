package com.careerforge.interview.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.careerforge.interview.entity.Interview;
import com.careerforge.interview.mapper.InterviewMapper;
import org.springframework.stereotype.Service;

/**
 * 面试中心服务
 * 负责面试表的 CRUD 操作
 *
 * @author Azir
 */
@Service
public class InterviewCenterService extends ServiceImpl<InterviewMapper, Interview> {

    // 继承自 ServiceImpl 的基础 CRUD 方法已足够使用
    // 业务逻辑请调用 InterviewCenterHandler

}
