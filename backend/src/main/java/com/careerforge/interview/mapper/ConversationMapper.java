package com.careerforge.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careerforge.interview.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 面试对话Mapper接口
 *
 * @author Azir
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

}
