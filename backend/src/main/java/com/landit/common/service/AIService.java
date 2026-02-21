package com.landit.common.service;

import com.alibaba.cloud.ai.dashscope.api.DashScopeResponseFormat;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.enums.Gender;
import com.landit.resume.dto.ResumeParseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * AI 服务
 * 基于 Spring AI Alibaba 实现大模型对话能力
 *
 * @author Azir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private final ChatClient chatClient;
    private final FileToImageService fileToImageService;
    private final AIPromptProperties promptProperties;
    private final ObjectMapper objectMapper;

    /**
     * 从文件中解析简历信息
     * 支持图片、PDF等格式的简历文件，使用大模型进行多模态识别
     *
     * @param file 上传的简历文件（图片或PDF）
     * @return 解析后的简历信息
     */
    public ResumeParseResult parseResumeFromFile(MultipartFile file) {
        log.info("开始解析简历文件: {}", file.getOriginalFilename());
        // 将文件转换为Media列表
        List<Media> mediaList = fileToImageService.convertToMedia(file);
        // 从配置中获取解析提示词
        String parsePrompt = promptProperties.getResume().getParse();
        // 创建UserMessage，包含文本和媒体
        UserMessage userMessage = UserMessage.builder()
                .text(parsePrompt)
                .media(mediaList)
                .build();
        // 创建Prompt并调用大模型
        Prompt prompt = new Prompt(List.of(userMessage));
        String jsonResponse = chatClient.prompt(prompt)
                .options(DashScopeChatOptions.builder()
                        .multiModel(true)
                        .incrementalOutput(true)
                        .responseFormat(DashScopeResponseFormat.builder()
                                .type(DashScopeResponseFormat.Type.JSON_SCHEMA)
                                .jsonScheme(DashScopeResponseFormat.JsonSchemaConfig.builder()
                                        .name("ResumeParseResult")
                                        .strict(true)
                                        .schema(Map.of(
                                                "type", "object",
                                                "properties", Map.of(
                                                        "name", Map.of(
                                                                "type", "string",
                                                                "description", "简历中的姓名"
                                                        ),
                                                        "gender", Map.of(
                                                                "type", "string",
                                                                "description", "性别",
                                                                "enum", List.of("男", "女", "未知")
                                                        ),
                                                        "markdownContent", Map.of(
                                                                "type", "string",
                                                                "description", "markdown格式的简历完整内容"
                                                        )
                                                ),
                                                "required", List.of("name", "gender", "markdownContent")
                                        ))
                                        .build())
                                .build())
                        .build())
                .call()
                .content();
        // 解析JSON响应
        ResumeParseResult result = parseResumeJsonResponse(jsonResponse);
        log.info("简历文件解析完成: 姓名={}", result.getName());
        return result;
    }

    /**
     * 解析简历JSON响应
     *
     * @param jsonResponse 大模型返回的JSON字符串
     * @return 解析后的ResumeParseResult对象
     */
    private ResumeParseResult parseResumeJsonResponse(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            String name = rootNode.has("name") ? rootNode.get("name").asText() : "";
            String genderText = rootNode.has("gender") ? rootNode.get("gender").asText() : "未知";
            String markdownContent = rootNode.has("markdownContent") ? rootNode.get("markdownContent").asText() : "";
            // 将性别文本转换为枚举
            Gender gender = Gender.fromText(genderText);
            return ResumeParseResult.builder()
                    .name(name)
                    .gender(gender)
                    .rawText(markdownContent)
                    .build();
        } catch (Exception e) {
            log.error("解析简历JSON响应失败: {}", jsonResponse, e);
            throw new RuntimeException("解析简历响应失败", e);
        }
    }

}
