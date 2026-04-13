package com.landit.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * ChatClient 工具类
 * 提供统一的同步调用大模型方法
 *
 * @author Azir
 */
@Slf4j
public final class ChatClientHelper {

    /**
     * AI 调用解析最大重试次数
     */
    private static final int MAX_RETRY = 3;

    /**
     * JSON 序列化工具
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ChatClientHelper() {
        // 工具类禁止实例化
    }

    // ==================== 基础调用方法 ====================

    // ==================== JSON Schema 约束方法 ====================

    /**
     * 调用 AI 并解析 JSON 响应为指定类型（拆分提示词版本，带自动重试）
     * Schema 名称自动使用类的简单名称，Schema 通过 @SchemaField 注解自动生成
     * 推荐使用此方法以利用前缀缓存优化
     *
     * @param chatClient    ChatClient 实例
     * @param systemPrompt  系统提示词（固定部分，可被缓存），可为 null
     * @param userPrompt    用户提示词（动态部分）
     * @param responseClass 响应类的 Class 对象（需标注 @SchemaField 注解）
     * @return 解析后的实体对象
     * @throws IllegalStateException 当重试次数用尽仍无法解析时抛出
     */
    public static <T> T callAndParse(
            ChatClient chatClient,
            String systemPrompt,
            String userPrompt,
            Class<T> responseClass) {

        Map<String, Object> schema = SchemaGenerator.fromClass(responseClass);
        String schemaJson = toJsonSchemaString(schema);

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                String response = callWithSchemaInternal(chatClient, systemPrompt, userPrompt, schemaJson);
                log.debug("大模型响应：{}", response);
                return JsonParseHelper.parseToEntity(response, responseClass);
            } catch (Exception e) {
                log.warn("AI 调用解析失败，第 {}/{} 次尝试，错误: {}", attempt, MAX_RETRY, e.getMessage());
                if (attempt >= MAX_RETRY) {
                    throw new IllegalStateException("AI 响应解析失败，已重试 " + MAX_RETRY + " 次", e);
                }
            }
        }

        throw new IllegalStateException("不应到达此处");
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 内部方法：带 JSON Schema 调用 AI（同步调用）
     */
    private static String callWithSchemaInternal(
            ChatClient chatClient,
            String systemPrompt,
            String userPrompt,
            String schemaJson) {

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .responseFormat(ResponseFormat.builder()
                        .type(ResponseFormat.Type.JSON_SCHEMA)
                        .jsonSchema(ResponseFormat.JsonSchema.builder()
                                .schema(schemaJson)
                                .strict(true)
                                .build())
                        .build())
                .build();

        var promptSpec = chatClient.prompt().options(options);
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            promptSpec.system(systemPrompt);
        }

        return promptSpec
                .user(userPrompt)
                .call()
                .content();
    }

    /**
     * 将 Map 格式的 Schema 转换为 JSON 字符串
     */
    private static String toJsonSchemaString(Map<String, Object> schema) {
        try {
            return OBJECT_MAPPER.writeValueAsString(schema);
        } catch (Exception e) {
            log.error("Schema 序列化失败", e);
            return "{}";
        }
    }

    // ==================== 提示词处理工具方法 ====================

    /**
     * 时间格式化器（精确到秒）
     */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 替换提示词模板中的变量占位符
     * 占位符格式：{variableName}
     * 自动在模板最前面添加当前时间戳
     *
     * @param template 提示词模板
     * @param variables 变量键值对
     * @return 替换后的提示词（带时间戳前缀）
     */
    public static String renderTemplate(String template, Map<String, String> variables) {
        if (template == null) {
            return null;
        }

        // 在模板最前面添加当前时间
        String currentTime = LocalDateTime.now().format(TIME_FORMATTER);
        String result = "[当前时间：" + currentTime + "]\n\n" + template;

        // 替换变量
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                result = result.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return result;
    }
}
