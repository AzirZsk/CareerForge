package com.landit.chat.service;

import com.landit.chat.dto.ChatEvent;
import com.landit.chat.dto.ChatRequest;
import com.landit.chat.dto.SectionChange;
import com.landit.common.service.FileToImageService;
import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.handler.ResumeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * AI聊天服务
 * 处理聊天请求并返回流式响应
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

    /**
     * 处理聊天请求，返回Flux流
     *
     * @param request 聊天请求
     * @param imageData 图片数据（可选）
     * @return SSE事件流
     */
    public Flux<ChatEvent> chat(ChatRequest request, byte[] imageData) {
        try {
            // 处理图片（如果有）
            List<Media> mediaList = new ArrayList<>();
            if (imageData != null && imageData.length > 0) {
                log.info("[AIChat] 处理上传图片，大小: {} bytes", imageData.length);
                // 这里简化处理，实际使用时需要传入MultipartFile
            }

            // 加载简历上下文（如果提供了resumeId）
            String resumeContext = "";
            if (request.getResumeId() != null && !request.getResumeId().isEmpty()) {
                resumeContext = loadResumeContext(request.getResumeId());
            }

            // 构建系统提示词
            String systemPrompt = buildSystemPrompt(resumeContext);

            // 构建用户消息（包含历史上下文）
            String userMessage = buildUserMessage(request);

            log.info("[AIChat] 开始AI对话: resumeId={}, message={}", request.getResumeId(), request.getCurrentUserMessage());

            // 调用ChatClient流式输出
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
                        log.debug("[AIChat] 收到chunk: {}", chunk);
                        return ChatEvent.chunk(chunk);
                    })
                    .concatWith(Flux.just(ChatEvent.complete("对话完成")))
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
     * 构建系统提示词
     */
    private String buildSystemPrompt(String resumeContext) {
        return """
                你是一位专业的简历优化顾问，帮助用户通过对话方式优化简历内容。

                # 角色定义
                你是一位经验丰富的简历优化专家，擅长：
                - 分析简历问题，给出专业诊断
                - 优化工作经历、项目描述等内容
                - 提供量化的成果描述建议
                - 帮助用户突出核心竞争力

                # 当前简历数据
                """ + resumeContext + """

                # 对话策略
                1. 首先理解用户需求（优化目标、目标岗位等）
                2. 根据需求提供具体、可操作的建议
                3. 如果用户询问简历相关内容，基于上述简历上下文回答
                4. 保持简洁明了，每次回复控制在200字以内
                5. 语气友好专业，使用中文回复

                # 注意事项
                - 不要编造不存在的简历信息
                - 如果用户没有选择简历，提示用户先选择一份简历
                - 提供的建议要具体、可执行
                """;
    }

    /**
     * 构建用户消息（包含历史上下文）
     */
    private String buildUserMessage(ChatRequest request) {
        StringBuilder message = new StringBuilder();

        // 添加历史对话上下文
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            message.append("【历史对话】\n");
            for (ChatRequest.ChatMessage msg : request.getMessages()) {
                String role = "user".equals(msg.getRole()) ? "用户" : "AI";
                message.append(role).append("：").append(msg.getContent()).append("\n");
            }
            message.append("\n");
        }

        // 添加当前用户消息
        message.append("【当前问题】\n");
        message.append(request.getCurrentUserMessage());

        return message.toString();
    }

    /**
     * 应用修改建议到简历
     *
     * @param resumeId 简历ID
     * @param changes 修改列表
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
