package com.landit.chat.service;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.landit.chat.dto.ChatEvent;
import com.landit.chat.dto.ChatStreamRequest;
import com.landit.chat.dto.SectionChange;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.enums.SectionType;
import com.landit.common.service.FileToImageService;
import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.handler.ResumeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;

/**
 * AI聊天服务
 * 使用 ReactAgent 处理聊天请求并返回流式响应
 * 支持多轮对话上下文（通过 MemorySaver）和图片输入
 *
 * @author Azir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIChatService {

    private final ReactAgent chatAgent;
    private final ResumeHandler resumeHandler;
    private final FileToImageService fileToImageService;
    private final AIPromptProperties aiPromptProperties;
    private final ChatMessageService chatMessageService;

    /**
     * 处理聊天请求，返回Flux流
     * 使用 ReactAgent 进行对话，支持多轮对话上下文和图片输入
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

            // 2. 构建 instruction（包含简历上下文）
            String instruction = buildInstruction(request);

            // 3. 处理图片（如果有）
            List<Media> mediaList = processImage(request.getImage());
            if (!mediaList.isEmpty()) {
                log.info("[AIChat] 检测到图片输入，共 {} 张", mediaList.size());
            }

            // 4. 构建运行时配置（使用 resumeId 作为 threadId 维护对话上下文）
            RunnableConfig config = RunnableConfig.builder()
                    .threadId(resumeId != null && !resumeId.isEmpty() ? resumeId : "default-chat-thread")
                    .build();

            log.info("[AIChat] 开始 Agent 对话: resumeId={}, threadId={}, hasImage={}",
                    resumeId, config.threadId(), !mediaList.isEmpty());

            // 5. 用于收集 AI 回复的 StringBuilder
            StringBuilder aiResponse = new StringBuilder();

            // 6. 构建 UserMessage（包含文本和图片）
            UserMessage userMessage = UserMessage.builder()
                    .text(instruction)
                    .media(mediaList)
                    .build();

            // 7. 调用 Agent 流式输出
            return chatAgent.stream(userMessage, config)
                    .filter(output -> output instanceof StreamingOutput)
                    .map(output -> (StreamingOutput) output)
                    .filter(so -> so.getOutputType() == OutputType.AGENT_MODEL_STREAMING)
                    .map(so -> {
                        String chunk = so.message().getText();
                        if (chunk != null && !chunk.isEmpty()) {
                            aiResponse.append(chunk);
                            log.debug("[AIChat] 收到chunk: {}", chunk);
                            return ChatEvent.chunk(chunk);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .concatWith(Flux.defer(() -> {
                        // 流结束后保存 AI 回复到数据库
                        if (resumeId != null && !resumeId.isEmpty()) {
                            chatMessageService.saveMessage(resumeId, "assistant", aiResponse.toString());
                        }
                        return Flux.just(ChatEvent.complete("对话完成"));
                    }))
                    .onErrorResume(e -> {
                        log.error("[AIChat] Agent 对话异常", e);
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
     * 构建 instruction（包含简历上下文）
     * 历史对话由 ReactAgent 的 MemorySaver 自动维护，无需手动加载
     * 简历上下文只在首次对话时注入，后续对话只发送用户问题
     */
    private String buildInstruction(ChatStreamRequest request) {
        StringBuilder instruction = new StringBuilder();
        String resumeId = request.getResumeId();

        // 只有首次对话才注入简历上下文
        boolean isFirstMessage = chatMessageService.isFirstMessage(resumeId);
        if (isFirstMessage && resumeId != null && !resumeId.isEmpty()) {
            String resumeContext = loadResumeContext(resumeId);
            String template = aiPromptProperties.getChat().getAdvisorConfig().getUserPromptTemplate();
            instruction.append(template.replace("{resumeContext}", resumeContext)).append("\n\n");
        }

        // 当前用户问题
        instruction.append(request.getCurrentUserMessage());

        return instruction.toString();
    }

    /**
     * 加载简历上下文
     * 遍历所有区块，拼接完整内容
     */
    private String loadResumeContext(String resumeId) {
        try {
            ResumeDetailVO resume = resumeHandler.getResumeDetail(resumeId);
            if (resume == null) {
                return "（简历未找到）";
            }

            StringBuilder context = new StringBuilder();

            // 1. 基本信息和评分
            context.append("## 简历基本信息\n");
            context.append("- 简历名称：").append(resume.getName()).append("\n");
            context.append("- 目标岗位：").append(resume.getTargetPosition()).append("\n");
            if (resume.getOverallScore() != null) {
                context.append("- 整体评分：").append(resume.getOverallScore()).append("分\n");
            }
            context.append("\n");

            // 2. 遍历区块，拼接内容
            if (resume.getSections() != null && !resume.getSections().isEmpty()) {
                context.append("## 简历区块内容\n\n");
                for (var section : resume.getSections()) {
                    String typeLabel = getSectionTypeLabel(section.getType());
                    context.append("### ").append(typeLabel);
                    if (section.getTitle() != null && !section.getTitle().isEmpty()) {
                        context.append(" - ").append(section.getTitle());
                    }
                    if (section.getScore() != null) {
                        context.append("（评分：").append(section.getScore()).append("分）");
                    }
                    context.append("\n");

                    // 区块内容
                    if (section.getContent() != null && !section.getContent().isEmpty()) {
                        context.append(section.getContent()).append("\n");
                    }
                    context.append("\n");
                }
            }

            return context.toString();
        } catch (Exception e) {
            log.error("[AIChat] 加载简历上下文失败", e);
            return "（加载简历上下文失败）";
        }
    }

    /**
     * 获取区块类型的中文标签
     */
    private String getSectionTypeLabel(String type) {
        SectionType sectionType = SectionType.fromCode(type);
        return sectionType != null ? sectionType.getDescription() : type;
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
