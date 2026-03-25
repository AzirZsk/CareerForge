package com.landit.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * AI聊天流式请求DTO
 * 使用 @ModelAttribute 接收的请求对象
 * 支持两种模式：简历对话（resumeId）和通用聊天（sessionId）
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatStreamRequest {

    /**
     * 简历ID（可选，用于提供简历上下文）
     */
    private String resumeId;

    /**
     * 会话ID（通用聊天模式使用UUID，简历模式可省略）
     */
    private String sessionId;

    /**
     * 当前用户消息
     */
    private String currentUserMessage;

    /**
     * 上传的图片/文件（可选）
     */
    private MultipartFile image;
}
