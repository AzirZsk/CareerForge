package com.careerforge.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careerforge.chat.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI聊天消息 Mapper
 * 继承 MyBatis-Plus BaseMapper，使用 LambdaQueryWrapper 在 Service 层查询
 *
 * @author Azir
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
