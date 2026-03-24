package com.landit.chat.service;

import com.landit.chat.dto.ChatEvent;
import com.landit.chat.dto.ChatStreamRequest;
import com.landit.chat.dto.SectionChange;
import com.landit.chat.entity.ChatMessage;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.service.FileToImageService;
import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.handler.ResumeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI聊天服务
 * 处理聊天请求并返回流式响应
 * 消息历史存储在数据库中，刷新页面不丢失
 *
 * @author Azir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIChatService {

    private final ChatClient chatClient;
    private final ResumeHandler resumeHandler;
    private final FileToImageService fileToImageService;
    private final AIPromptProperties aiPromptProperties;
    private final ChatMessageService chatMessageService;

    /**
     * 处理聊天请求，返回Flux流
     * 消息历史从数据库加载，AI回复后保存到数据库
     *
     * @param request 聊天请求（包含图片字段）
     * @return SSE事件流
     */
    public Flux<ChatEvent> chat(ChatStreamRequest request) {
        try {
            String resumeId = request.getResumeId();

            // 1. 保存用户消息到数据库（如果有 resumeId）
            if (resumeId != null && !resumeId.isEmpty()) {
                chatMessageService.saveMessage(resumeId, "user", request.getCurrentUserMessage());
            }

            // 2. 处理图片
            List<Media> mediaList = processImage(request.getImage());

            // 3. 加载简历上下文
            String resumeContext = loadResumeContextIfNeeded(resumeId);

            // 4. 从数据库加载历史消息
            String historyContext = loadHistoryContext(resumeId);

            // 5. 构建提示词
            String systemPrompt = aiPromptProperties.getChat().getAdvisorConfig().getSystemPrompt();
            String userMessage = buildUserMessage(request, resumeContext, historyContext);

            log.info("[AIChat] 开始AI对话: resumeId={}, message={}", resumeId, request.getCurrentUserMessage());

            // 6. 用于收集 AI 回复的 StringBuilder
            StringBuilder aiResponse = new StringBuilder();

            // 7. 调用 AI，流式返回
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userSpec -> {
                        userSpec.text(userMessage);
                        if (!mediaList.isEmpty()) {
                            userSpec.media(mediaList.toArray(new Media[0]));
                        }
                    })
                    .stream()
                    .content()
                    .map(chunk -> {
                        aiResponse.append(chunk);  // 收集 AI 回复
                        log.debug("[AIChat] 收到chunk: {}", chunk);
                        return ChatEvent.chunk(chunk);
                    })
                    .concatWith(Flux.defer(() -> {
                        // 流结束后保存 AI 回复到数据库
                        if (resumeId != null && !resumeId.isEmpty()) {
                            chatMessageService.saveMessage(resumeId, "assistant", aiResponse.toString());
                        }
                        return Flux.just(ChatEvent.complete("对话完成"));
                    }))
                    .onErrorResume(e -> {
                        log.error("[AIChat] 对话异常", e);
                        return Flux.just(ChatEvent.error("对话处理失败: " + e.getMessage()));
                    });

        } catch (Exception e) {
            log.error("[AIChat] 处理请求失败", e);
            return Flux.just(ChatEvent.error("处理请求失败: " + e.getMessage()));
        }
    }

    /**
     * 处理上传的图片/文件
     */
    private List<Media> processImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return List.of();
        }
        log.info("[AIChat] 处理上传图片: {}, 大小: {} bytes", image.getOriginalFilename(), image.getSize());
        return fileToImageService.convertToMedia(image);
    }

    /**
     * 按需加载简历上下文
     */
    private String loadResumeContextIfNeeded(String resumeId) {
        if (resumeId == null || resumeId.isEmpty()) {
            return "";
        }
        return loadResumeContext(resumeId);
    }

    /**
     * 加载简历上下文
     */
    private String loadResumeContext(String resumeId) {
        try {
            ResumeDetailVO resume = resumeHandler.getResumeDetail(resumeId);
            if (resume == null) {
                return "（简历未找到）";
            }

            StringBuilder context = new StringBuilder();
            context.append("## 简历基本信息\n");
            context.append("- 简历名称：").append(resume.getName()).append("\n");
            context.append("- 目标岗位：").append(resume.getTargetPosition()).append("\n");
            context.append("- 整体评分：").append(resume.getOverallScore()).append("分\n");
            context.append("- 内容评分：").append(resume.getContentScore()).append("分\n");
            context.append("- 结构评分：").append(resume.getStructureScore()).append("分\n");
            context.append("- 匹配度评分：").append(resume.getMatchingScore()).append("分\n");
            context.append("- 竞争力评分：").append(resume.getCompetitivenessScore()).append("分\n\n");

            context.append("## 简历区块列表\n");
            if (resume.getSections() != null) {
                for (var section : resume.getSections()) {
                    context.append("- **").append(section.getType()).append("**");
                    if (section.getTitle() != null && !section.getTitle().isEmpty()) {
                        context.append("（").append(section.getTitle()).append("）");
                    }
                    context.append(" 评分：").append(section.getScore()).append("分\n");
                }
            }

            return context.toString();
        } catch (Exception e) {
            log.error("[AIChat] 加载简历上下文失败", e);
            return "（加载简历上下文失败）";
        }
    }

    /**
     * 从数据库加载历史消息上下文
     */
    private String loadHistoryContext(String resumeId) {
        if (resumeId == null || resumeId.isEmpty()) {
            return "";
        }

        List<ChatMessage> history = chatMessageService.getHistory(resumeId, 20);
        if (history.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("【历史对话】\n");
        for (ChatMessage msg : history) {
            String role = "user".equals(msg.getRole()) ? "用户" : "AI";
            context.append(role).append("：").append(msg.getContent()).append("\n");
        }
        context.append("\n");

        return context.toString();
    }

    /**
     * 构建用户消息（包含简历上下文和历史对话）
     */
    private String buildUserMessage(ChatStreamRequest request, String resumeContext, String historyContext) {
        StringBuilder message = new StringBuilder();

        if (!resumeContext.isEmpty()) {
            String template = aiPromptProperties.getChat().getAdvisorConfig().getUserPromptTemplate();
            message.append(template.replace("{resumeContext}", resumeContext)).append("\n\n");
        }

        if (!historyContext.isEmpty()) {
            message.append(historyContext);
        }

        message.append("【当前问题】\n").append(request.getCurrentUserMessage());

        return message.toString();
    }

    /**
     * 应用修改建议到简历
     *
     * @param resumeId 简历ID
     * @param changes  修改列表
     */
    public void applyChanges(String resumeId, List<SectionChange> changes) {
        log.info("[AIChat] 应用修改: resumeId={}, changes={}", resumeId, changes.size());

        // TODO: 实现应用修改逻辑
        // 1. 遍历changes
        // 2. 根据changeType调用相应的Service方法
        // 3. 更新/新增/删除区块

        for (SectionChange change : changes) {
            switch (change.getChangeType()) {
                case "update" -> {
                    log.info("[AIChat] 更新区块: sectionId={}", change.getSectionId());
                    // resumeHandler.updateSection(resumeId, change.getSectionId(), change.getAfterContent());
                }
                case "add" -> {
                    log.info("[AIChat] 新增区块: type={}", change.getSectionType());
                    // resumeHandler.addSection(resumeId, change.getSectionType(), change.getAfterContent());
                }
                case "delete" -> {
                    log.info("[AIChat] 删除区块: sectionId={}", change.getSectionId());
                    // resumeHandler.deleteSection(resumeId, change.getSectionId());
                }
                default -> log.warn("[AIChat] 未知的变更类型: {}", change.getChangeType());
            }
        }
    }
}
