package com.landit.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI聊天请求DTO
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    /**
     * 简历ID（可选，用于提供上下文）
     */
    private String resumeId;

    /**
     * 历史消息列表
     */
    private List<ChatMessage> messages;

    /**
     * 当前用户消息
     */
    private String currentUserMessage;

    /**
     * 聊天消息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessage {
        /**
         * 角色：user / assistant
         */
        private String role;

        /**
         * 消息内容
         */
        private String content;

        /**
         * 是否包含图片
         */
        private Boolean hasImage;
    }
}
