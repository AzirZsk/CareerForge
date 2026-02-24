package com.landit.common.service;

import com.alibaba.cloud.ai.dashscope.api.DashScopeResponseFormat;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.exception.BusinessException;
import com.landit.common.enums.Gender;
import com.landit.common.schema.SectionSchemaRegistry;
import com.landit.resume.dto.ResumeParseResult;
import com.landit.resume.dto.ResumeStructuredData;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
    private final SectionSchemaRegistry schemaRegistry;

    /**
     * 从文件中解析简历信息
     * 支持图片、PDF等格式的简历文件，使用大模型进行多模态识别
     * 使用流式请求，在服务层收集完整响应后返回
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
        // 创建Prompt并调用大模型（使用流式请求）
        Prompt prompt = new Prompt(List.of(userMessage));
        Flux<String> streamResponse = chatClient.prompt(prompt)
                .options(DashScopeChatOptions.builder()
                        .multiModel(true)
                        .incrementalOutput(true)
                        .responseFormat(DashScopeResponseFormat.builder()
                                .type(DashScopeResponseFormat.Type.JSON_SCHEMA)
                                .jsonScheme(DashScopeResponseFormat.JsonSchemaConfig.builder()
                                        .name("ResumeParseResult")
                                        .strict(true)
                                        .schema(schemaRegistry.buildResumeSchema())
                                        .build())
                                .build())
                        .build())
                .stream()
                .content();
        // 收集流式响应的所有块，拼接为完整JSON
        String jsonResponse = streamResponse
                .collectList()
                .map(chunks -> String.join("", chunks))
                .block();
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
            JsonNode root = objectMapper.readTree(jsonResponse);

            ResumeStructuredData structuredData = parseStructuredData(root);
            String name = getTextOrEmpty(root, "name");
            Gender gender = Gender.fromText(getTextOrDefault(root, "gender", "未知"));

            return ResumeParseResult.builder()
                    .name(name)
                    .gender(gender)
                    .rawText(getTextOrEmpty(root, "markdownContent"))
                    .structuredData(structuredData)
                    .build();
        } catch (Exception e) {
            log.error("解析简历JSON响应失败: {}", jsonResponse, e);
            throw BusinessException.serverError("解析简历响应失败");
        }
    }

    /**
     * 解析结构化简历数据
     */
    private ResumeStructuredData parseStructuredData(JsonNode root) {
        JsonNode basicInfoNode = root.get("basicInfo");

        return ResumeStructuredData.builder()
                .basicInfo(parseBasicInfo(basicInfoNode))
                .education(parseArrayNode(root.get("education"), this::parseEducation))
                .work(parseArrayNode(root.get("work"), this::parseWork))
                .projects(parseArrayNode(root.get("projects"), this::parseProject))
                .skills(parseStringList(root.get("skills")))
                .certificates(parseArrayNode(root.get("certificates"), this::parseCertificate))
                .openSource(parseArrayNode(root.get("openSource"), this::parseOpenSource))
                .build();
    }

    /**
     * 解析基本信息
     */
    private ResumeStructuredData.BasicInfo parseBasicInfo(JsonNode node) {
        if (node == null || node.isNull()) {
            return ResumeStructuredData.BasicInfo.builder().build();
        }
        return ResumeStructuredData.BasicInfo.builder()
                .name(getTextOrEmpty(node, "name"))
                .gender(Gender.fromText(getTextOrDefault(node, "gender", "未知")))
                .phone(getTextOrEmpty(node, "phone"))
                .email(getTextOrEmpty(node, "email"))
                .targetPosition(getTextOrEmpty(node, "targetPosition"))
                .summary(getTextOrEmpty(node, "summary"))
                .build();
    }

    /**
     * 解析教育经历
     */
    private ResumeStructuredData.EducationExperience parseEducation(JsonNode node) {
        return ResumeStructuredData.EducationExperience.builder()
                .school(getTextOrEmpty(node, "school"))
                .degree(getTextOrEmpty(node, "degree"))
                .major(getTextOrEmpty(node, "major"))
                .period(getTextOrEmpty(node, "period"))
                .build();
    }

    /**
     * 解析工作经历
     */
    private ResumeStructuredData.WorkExperience parseWork(JsonNode node) {
        return ResumeStructuredData.WorkExperience.builder()
                .company(getTextOrEmpty(node, "company"))
                .position(getTextOrEmpty(node, "position"))
                .period(getTextOrEmpty(node, "period"))
                .description(getTextOrEmpty(node, "description"))
                .build();
    }

    /**
     * 解析项目经验
     */
    private ResumeStructuredData.ProjectExperience parseProject(JsonNode node) {
        return ResumeStructuredData.ProjectExperience.builder()
                .name(getTextOrEmpty(node, "name"))
                .role(getTextOrEmpty(node, "role"))
                .period(getTextOrEmpty(node, "period"))
                .description(getTextOrEmpty(node, "description"))
                .achievements(parseStringList(node.get("achievements")))
                .build();
    }

    /**
     * 解析证书
     */
    private ResumeStructuredData.Certificate parseCertificate(JsonNode node) {
        return ResumeStructuredData.Certificate.builder()
                .name(getTextOrEmpty(node, "name"))
                .date(getTextOrEmpty(node, "date"))
                .build();
    }

    /**
     * 解析开源贡献
     */
    private ResumeStructuredData.OpenSourceContribution parseOpenSource(JsonNode node) {
        return ResumeStructuredData.OpenSourceContribution.builder()
                .projectName(getTextOrEmpty(node, "projectName"))
                .url(getTextOrEmpty(node, "url"))
                .role(getTextOrEmpty(node, "role"))
                .period(getTextOrEmpty(node, "period"))
                .description(getTextOrEmpty(node, "description"))
                .achievements(parseStringList(node.get("achievements")))
                .build();
    }

    /**
     * 通用数组解析方法
     */
    private <T> List<T> parseArrayNode(JsonNode node, Function<JsonNode, T> parser) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        return StreamSupport.stream(node.spliterator(), false)
                .map(parser)
                .collect(Collectors.toList());
    }

    /**
     * 解析字符串列表
     */
    private List<String> parseStringList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        return StreamSupport.stream(node.spliterator(), false)
                .map(JsonNode::asText)
                .collect(Collectors.toList());
    }

    /**
     * 获取文本字段，若不存在返回空字符串
     */
    private String getTextOrEmpty(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asText() : "";
    }

    /**
     * 获取文本字段，若不存在返回默认值
     */
    private String getTextOrDefault(JsonNode node, String field, String defaultValue) {
        return node.has(field) ? node.get(field).asText() : defaultValue;
    }

}
