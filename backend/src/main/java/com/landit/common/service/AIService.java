package com.landit.common.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.exception.BusinessException;
import com.landit.common.schema.SectionSchemaRegistry;
import com.landit.common.util.JsonParseHelper;
import com.landit.resume.dto.ResumeStructuredData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * AI 服务
 * 基于 Spring AI OpenAI 实现大模型对话能力
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
    private final SectionSchemaRegistry schemaRegistry;

    // ==================== 简历解析相关方法 ====================

    /**
     * 从文件中解析简历信息
     * 支持图片、PDF等格式的简历文件，使用大模型进行多模态识别
     * 使用同步调用，配合 OpenAI ResponseFormat 约束输出格式
     *
     * @param file 上传的简历文件（图片或PDF）
     * @return 解析后的简历信息
     */
    public ResumeStructuredData parseResumeFromFile(MultipartFile file) {
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
        // 构建 JSON Schema 约束
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .responseFormat(ResponseFormat.builder()
                        .type(ResponseFormat.Type.JSON_SCHEMA)
                        .jsonSchema(ResponseFormat.JsonSchema.builder()
                                .strict(true)
                                .schema(schemaRegistry.buildResumeSchema())
                                .name("resumeSchema")
                                .build())
                        .build())
                .build();
        // OpenAI: 同步调用
        String jsonResponse = chatClient.prompt()
                .messages(userMessage)
                .options(options)
                .call()
                .content();
        // 解析JSON响应
        ResumeStructuredData result = parseResumeJsonResponse(jsonResponse);
        log.info("简历文件解析完成:{}", JsonParseHelper.toJsonString(result));
        return result;
    }

    /**
     * 解析简历JSON响应
     * 直接使用 Jackson 反序列化
     *
     * @param jsonResponse 大模型返回的JSON字符串
     * @return 解析后的 ResumeStructuredData 对象
     */
    private ResumeStructuredData parseResumeJsonResponse(String jsonResponse) {
        try {
            return objectMapper.readValue(jsonResponse, ResumeStructuredData.class);
        } catch (Exception e) {
            log.error("解析简历JSON响应失败: {}", jsonResponse, e);
            throw BusinessException.serverError("解析简历响应失败");
        }
    }

}
