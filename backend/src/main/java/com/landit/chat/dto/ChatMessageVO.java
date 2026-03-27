package com.landit.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI聊天消息 VO
 * 返回给前端的聊天消息对象
 *
 * @author Azir
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageVO {

    /**
     * 消息ID
     */
    private String id;

    /**
     * 角色（user / assistant）
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 操作卡片列表（AI建议的简历修改）
     */
    private List<SectionChange> actions;

    /**
     * 操作状态（pending-待应用 / applied-已应用 / failed-应用失败）
     */
    private String actionStatus;
}
