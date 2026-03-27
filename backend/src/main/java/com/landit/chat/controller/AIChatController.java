package com.landit.chat.controller;

import com.landit.chat.dto.ApplyChangesRequest;
import com.landit.chat.dto.ChatMessageVO;
import com.landit.chat.dto.ChatStreamRequest;
import com.landit.chat.handler.AIChatHandler;
import com.landit.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI聊天控制器
 * 提供聊天接口和SSE流式输出
 * 支持两种模式：简历对话（resumeId）和通用聊天（sessionId）
 * 所有业务逻辑委托给 AIChatHandler 处理
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

    @Operation(summary = "流式聊天", description = "支持文本、图片、简历上下文，返回SSE流")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(
            @ModelAttribute ChatStreamRequest request,
            HttpServletResponse response) {
        log.info("[AIChat] 收到聊天请求: resumeId={}, sessionId={}, imageCount={}",
                request.getResumeId(),
                request.getSessionId(),
                request.getImages() != null ? request.getImages().size() : 0);
        return chatHandler.streamChat(request, response);
    }

    @Operation(summary = "获取聊天历史", description = "获取指定会话的聊天历史消息（含操作卡片）")
    @GetMapping("/history/{sessionId}")
    public ApiResponse<List<ChatMessageVO>> getHistory(@PathVariable String sessionId) {
        List<ChatMessageVO> voList = chatHandler.getHistory(sessionId);
        return ApiResponse.success(voList);
    }

    @Operation(summary = "更新消息操作状态", description = "更新消息的操作状态（pending/applied/failed）")
    @PatchMapping("/messages/{messageId}/status")
    public ApiResponse<Void> updateActionStatus(
            @PathVariable String messageId,
            @RequestParam String status) {
        log.info("[AIChat] 更新消息操作状态: messageId={}, status={}", messageId, status);
        chatHandler.updateActionStatus(messageId, status);
        return ApiResponse.success();
    }

    @Operation(summary = "清空聊天历史", description = "清空指定会话的聊天历史")
    @DeleteMapping("/history/{sessionId}")
    public ApiResponse<Void> clearHistory(@PathVariable String sessionId) {
        chatHandler.clearHistory(sessionId);
        log.info("[AIChat] 已清空会话 {} 的聊天历史", sessionId);
        return ApiResponse.success();
    }

    @Operation(summary = "应用修改", description = "批量更新简历区块")
    @PostMapping("/apply")
    public ApiResponse<Void> applyChanges(@RequestBody ApplyChangesRequest request) {
        chatHandler.applyChanges(request);
        return ApiResponse.success();
    }
}
