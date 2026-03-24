package com.landit.chat.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.chat.dto.ApplyChangesRequest;
import com.landit.chat.dto.ChatRequest;
import com.landit.chat.handler.AIChatHandler;
import com.landit.chat.service.AIChatService;
import com.landit.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * AI聊天控制器
 * 提供聊天接口和SSE流式输出
 *
 * @author Azir
 */
@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Tag(name = "AI聊天", description = "AI聊天对话优化简历接口")
public class AIChatController {

    private final AIChatHandler chatHandler;
    private final AIChatService chatService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "流式聊天", description = "支持文本、图片、简历上下文，返回SSE流")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(
            @RequestParam(required = false) String resumeId,
            @RequestParam String currentUserMessage,
            @RequestParam(required = false) String messages,
            @RequestParam(required = false) MultipartFile image,
            HttpServletResponse response) throws IOException {

        log.info("[AIChat] 收到聊天请求: resumeId={}, hasImage={}", resumeId, image != null && !image.isEmpty());

        ChatRequest request = ChatRequest.builder()
                .resumeId(resumeId)
                .currentUserMessage(currentUserMessage)
                .messages(parseMessages(messages))
                .build();

        byte[] imageData = null;
        if (image != null && !image.isEmpty()) {
            imageData = image.getBytes();
        }

        return chatHandler.streamChat(request, imageData, response);
    }

    @Operation(summary = "应用修改", description = "批量更新简历区块")
    @PostMapping("/apply")
    public ApiResponse<Void> applyChanges(@RequestBody ApplyChangesRequest request) {
        log.info("[AIChat] 应用修改请求: resumeId={}, changes={}", request.getResumeId(), request.getChanges().size());
        chatService.applyChanges(request.getResumeId(), request.getChanges());
        return ApiResponse.success();
    }

    private List<ChatRequest.ChatMessage> parseMessages(String messagesJson) {
        if (messagesJson == null || messagesJson.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            TypeReference<List<ChatRequest.ChatMessage>> typeRef = new TypeReference<>() {};
            return objectMapper.readValue(messagesJson, typeRef);
        } catch (Exception e) {
            log.warn("[AIChat] 解析历史消息失败，使用空列表", e);
            return Collections.emptyList();
        }
    }
}
