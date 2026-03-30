package com.landit.interview.voice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.landit.interview.voice.entity.AssistantConversation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 助手对话记录 Mapper
 *
 * @author Azir
 */
@Mapper
public interface AssistantConversationMapper extends BaseMapper<AssistantConversation> {
}
