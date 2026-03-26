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
import com.landit.resume.dto.AddSectionRequest;
import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.dto.UpdateSectionRequest;
import com.landit.resume.handler.ResumeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * AI聊天服务
 * 使用 ReactAgent 处理聊天请求并返回流式响应
 * 支持多轮对话上下文（通过 MemorySaver）和图片输入
 * 支持两种模式：简历对话（resumeId）和通用聊天（sessionId）
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
            String sessionId = request.getSessionId();

            // 1. 确定会话ID：简历模式使用 resumeId，通用模式使用或生成 sessionId
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = (resumeId != null && !resumeId.isEmpty())
                        ? resumeId
                        : java.util.UUID.randomUUID().toString();
            }

            final String finalSessionId = sessionId;

            // 2. 保存用户消息到数据库
            chatMessageService.saveMessage(finalSessionId, resumeId, "user", request.getCurrentUserMessage());

            // 3. 构建 instruction（包含简历上下文或通用提示词）
            String instruction = buildInstruction(request, finalSessionId);

            // 4. 处理图片（如果有）
            List<Media> mediaList = processImages(request.getImages());
            if (!mediaList.isEmpty()) {
                log.info("[AIChat] 检测到图片输入，共 {} 张", mediaList.size());
            }

            // 5. 构建运行时配置（使用 sessionId 作为 threadId 维护对话上下文）
            RunnableConfig config = RunnableConfig.builder()
                    .threadId(finalSessionId)
                    .build();

            log.info("[AIChat] 开始 Agent 对话: sessionId={}, resumeId={}, threadId={}, imageCount={}",
                    finalSessionId, resumeId, config.threadId(), mediaList.size());

            // 6. 用于收集 AI 回复的 StringBuilder
            StringBuilder aiResponse = new StringBuilder();

            // 7. 构建 UserMessage（包含文本和图片）
            UserMessage userMessage = UserMessage.builder()
                    .text(instruction)
                    .media(mediaList)
                    .build();

            // 8. 调用 Agent 流式输出
            return chatAgent.stream(userMessage, config)
                    .filter(output -> output instanceof StreamingOutput)
                    .map(output -> (StreamingOutput) output)
                    .filter(so -> so.getOutputType() == OutputType.AGENT_MODEL_STREAMING)
                    .flatMap(so -> {
                        String chunk = so.message().getText();
                        if (chunk != null && !chunk.isEmpty()) {
                            aiResponse.append(chunk);
                            return Flux.just(ChatEvent.chunk(chunk));
                        }
                        return Flux.empty();
                    })
                    .concatWith(Flux.defer(() -> {
                        // 流结束后保存 AI 回复到数据库
                        chatMessageService.saveMessage(finalSessionId, resumeId, "assistant", aiResponse.toString());
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
     * 处理上传的多张图片/文件
     *
     * @param images 图片列表
     * @return Media列表
     */
    private List<Media> processImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        List<Media> allMedia = new ArrayList<>();
        for (MultipartFile image : images) {
            if (image != null && !image.isEmpty()) {
                log.info("[AIChat] 处理上传图片: {}, 大小: {} bytes",
                        image.getOriginalFilename(), image.getSize());
                allMedia.addAll(fileToImageService.convertToMedia(image));
            }
        }
        log.info("[AIChat] 共处理 {} 张图片，生成 {} 个 Media 对象",
                images.size(), allMedia.size());
        return allMedia;
    }

    /**
     * 构建 instruction（包含简历上下文或通用提示词）
     * 历史对话由 ReactAgent 的 MemorySaver 自动维护，无需手动加载
     * 简历上下文只在首次对话时注入，后续对话只发送用户问题
     */
    private String buildInstruction(ChatStreamRequest request, String sessionId) {
        // 非首次对话：直接返回用户问题
        if (!chatMessageService.isFirstMessage(sessionId)) {
            return request.getCurrentUserMessage();
        }

        // 首次对话：注入上下文
        StringBuilder instruction = new StringBuilder();
        String resumeId = request.getResumeId();

        if (resumeId != null && !resumeId.isEmpty()) {
            // 简历模式：注入简历上下文
            String resumeContext = loadResumeContext(resumeId);
            String template = aiPromptProperties.getChat().getAdvisorConfig().getUserPromptTemplate();
            instruction.append(template.replace("{resumeContext}", resumeContext));
        } else {
            // 通用聊天模式：注入通用提示词
            instruction.append(buildGeneralContext());
        }

        instruction.append("\n\n").append(request.getCurrentUserMessage());
        return instruction.toString();
    }

    /**
     * 构建通用聊天模式的提示词（从配置文件读取）
     */
    private String buildGeneralContext() {
        String systemPrompt = aiPromptProperties.getChat().getGeneralConfig().getSystemPrompt();
        if (systemPrompt == null || systemPrompt.isBlank()) {
            // 兜底默认值
            return """
                你是 LandIt 求职助手，专门帮助用户进行求职相关咨询。
                你可以协助用户解答求职相关问题、创建简历、提供面试建议等。
                如果用户需要创建简历，请使用 create_resume 工具。
                """;
        }
        return systemPrompt;
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
            // 基本信息拼接
            context.append("## 简历基本信息\n");
            context.append("- 简历名称：").append(resume.getName()).append("\n");
            context.append("- 目标岗位：").append(resume.getTargetPosition()).append("\n");
            if (resume.getOverallScore() != null) {
                context.append("- 整体评分：").append(resume.getOverallScore()).append("分\n");
            }
            context.append("\n");

            // 区块内容拼接
            List<ResumeDetailVO.ResumeSectionVO> sections = resume.getSections();
            if (sections == null || sections.isEmpty()) {
                return context.toString();
            }

            context.append("## 简历区块内容\n\n");
            for (ResumeDetailVO.ResumeSectionVO section : sections) {
                appendSectionContent(context, section);
            }

            return context.toString();
        } catch (Exception e) {
            log.error("[AIChat] 加载简历上下文失败", e);
            return "（加载简历上下文失败）";
        }
    }

    /**
     * 拼接单个区块内容到上下文
     */
    private void appendSectionContent(StringBuilder context, ResumeDetailVO.ResumeSectionVO section) {
        String typeLabel = getSectionTypeLabel(section.getType());
        context.append("### ").append(typeLabel);

        String title = section.getTitle();
        if (title != null && !title.isEmpty()) {
            context.append(" - ").append(title);
        }

        Integer score = section.getScore();
        if (score != null) {
            context.append("（评分：").append(score).append("分）");
        }
        context.append("\n");

        String content = section.getContent();
        if (content != null && !content.isEmpty()) {
            context.append(content).append("\n");
        }
        context.append("\n");
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
     * 遍历修改列表，根据 changeType 调用相应的 Handler 方法
     *
     * @param resumeId 简历ID
     * @param changes  修改列表
     */
    public void applyChanges(String resumeId, List<SectionChange> changes) {
        log.info("[AIChat] 应用修改: resumeId={}, changes={}", resumeId, changes.size());

        for (SectionChange change : changes) {
            try {
                switch (change.getChangeType()) {
                    case "update" -> {
                        log.info("[AIChat] 更新区块: sectionId={}", change.getSectionId());
                        UpdateSectionRequest request = new UpdateSectionRequest();
                        request.setContent(change.getAfterContent());
                        resumeHandler.updateResumeSection(resumeId, change.getSectionId(), request);
                    }
                    case "add" -> {
                        log.info("[AIChat] 新增区块: type={}, title={}",
                                change.getSectionType(), change.getSectionTitle());
                        AddSectionRequest request = new AddSectionRequest();
                        request.setType(change.getSectionType());
                        request.setTitle(change.getSectionTitle());
                        request.setContent(change.getAfterContent());
                        resumeHandler.addResumeSection(resumeId, request);
                    }
                    case "delete" -> {
                        log.info("[AIChat] 删除区块: sectionId={}", change.getSectionId());
                        resumeHandler.deleteResumeSection(resumeId, change.getSectionId());
                    }
                    default -> log.warn("[AIChat] 未知的变更类型: {}", change.getChangeType());
                }
            } catch (Exception e) {
                log.error("[AIChat] 应用修改失败: change={}", change, e);
                throw new RuntimeException("应用修改失败: " + e.getMessage(), e);
            }
        }

        log.info("[AIChat] 所有修改已应用成功: resumeId={}", resumeId);
    }
}
