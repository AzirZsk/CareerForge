package com.careerforge.chat.dto;

import java.util.List;
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
     * 上传的图片列表（最多10张）
     */
    private List<MultipartFile> images;

    /**
     * 当前用户ID（由控制器从请求属性注入，不由前端传递）
     */
    private String userId;
}
