package com.careerforge.chat.dto;

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

    /**
     * 简历ID（该消息关联的简历，仅简历模式下有值）
     */
    private String resumeId;

    /**
     * 内容分片列表（有序：text和action交替，用于前端按穿插顺序渲染）
     */
    private List<ContentSegment> segments;

    /**
     * 内容分片，记录文字和操作卡片的穿插顺序
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentSegment {

        /**
         * 分片类型：text 或 action
         */
        private String type;

        /**
         * 文本内容（type=text时有值）
         */
        private String content;

        /**
         * 操作卡片索引（type=action时有值，指向actions数组的下标）
         */
        private Integer actionIndex;
    }
}
