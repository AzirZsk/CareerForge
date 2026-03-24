package com.landit.chat.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.landit.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI聊天消息实体
 * 持久化存储用户与AI的对话历史
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_chat_message")
public class ChatMessage extends BaseEntity {

    /**
     * 关联的简历ID
     */
    private String resumeId;

    /**
     * 角色（user / assistant）
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;
}
