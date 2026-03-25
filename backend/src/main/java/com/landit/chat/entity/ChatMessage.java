package com.landit.chat.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.landit.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI聊天消息实体
 * 持久化存储用户与AI的对话历史
 * 支持两种模式：简历对话（resumeId）和通用聊天（sessionId）
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_chat_message")
public class ChatMessage extends BaseEntity {

    /**
     * 关联的简历ID（可选，通用聊天模式为空）
     */
    private String resumeId;

    /**
     * 会话ID（通用聊天模式使用UUID，简历模式使用resumeId）
     */
    private String sessionId;

    /**
     * 角色（user / assistant）
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;
}
