package com.landit.common.util;

import com.alibaba.cloud.ai.dashscope.api.DashScopeResponseFormat;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * ChatClient 工具类
 * 提供统一的流式调用大模型方法
 *
 * @author Azir
 */
@Slf4j
public final class ChatClientHelper {

    /**
     * AI 调用解析最大重试次数
     */
    private static final int MAX_RETRY = 3;

    private ChatClientHelper() {
        // 工具类禁止实例化
    }

    /**
     * 默认的 DashScope 聊天选项配置
     * 启用多模态和增量输出
     */
    private static final DashScopeChatOptions DEFAULT_OPTIONS = DashScopeChatOptions.builder()
            .multiModel(true)
            .incrementalOutput(true)
            .build();

    /**
     * 使用流式请求调用大模型，收集完整响应
     *
     * @param chatClient ChatClient 实例
     * @param prompt     提示词
     * @return 完整的响应文本
     */
    public static String callStreamAndCollect(ChatClient chatClient, String prompt) {
        return callStreamAndCollect(chatClient, prompt, DEFAULT_OPTIONS);
    }

    /**
     * 使用流式请求调用大模型，收集完整响应（自定义选项）
     *
     * @param chatClient ChatClient 实例
     * @param prompt     提示词
     * @param options    自定义的 DashScope 选项
     * @return 完整的响应文本
     */
    public static String callStreamAndCollect(ChatClient chatClient, String prompt, DashScopeChatOptions options) {
        Flux<String> streamResponse = chatClient.prompt()
                .user(prompt)
                .options(options)
                .stream()
                .content();

        return streamResponse
                .collectList()
                .map(chunks -> String.join("", chunks))
                .block();
    }

    /**
     * 使用流式请求调用大模型，收集完整响应（带 JSON Schema 约束）
     * 强制大模型输出符合指定 Schema 的 JSON 格式
     *
     * @param chatClient ChatClient 实例
     * @param promptText 提示词
     * @param schema     JSON Schema 定义
     * @param schemaName Schema 名称（用于日志和调试）
     * @return 完整的 JSON 响应文本
     */
    public static String callStreamAndCollectWithSchema(
            ChatClient chatClient,
            String promptText,
            Map<String, Object> schema,
            String schemaName) {

        log.debug("使用 JSON Schema 调用大模型: schemaName={}", schemaName);

        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .multiModel(true)
                .incrementalOutput(true)
                .responseFormat(DashScopeResponseFormat.builder()
                        .type(DashScopeResponseFormat.Type.JSON_SCHEMA)
                        .jsonScheme(DashScopeResponseFormat.JsonSchemaConfig.builder()
                                .name(schemaName)
                                .strict(true)
                                .schema(schema)
                                .build())
                        .build())
                .build();

        Prompt prompt = new Prompt(promptText);
        Flux<String> streamResponse = chatClient.prompt(prompt)
                .options(options)
                .stream()
                .content();

        return streamResponse
                .collectList()
                .map(chunks -> String.join("", chunks))
                .block();
    }

    /**
     * 使用流式请求调用大模型，收集完整响应（带 JSON Schema 约束）
     * 使用 ChatClient.Builder 创建新的 ChatClient 实例
     *
     * @param chatClientBuilder ChatClient.Builder 实例
     * @param promptText        提示词
     * @param schema            JSON Schema 定义
     * @param schemaName        Schema 名称（用于日志和调试）
     * @return 完整的 JSON 响应文本
     */
    public static String callStreamAndCollectWithSchema(
            ChatClient.Builder chatClientBuilder,
            String promptText,
            Map<String, Object> schema,
            String schemaName) {

        ChatClient chatClient = chatClientBuilder.build();
        return callStreamAndCollectWithSchema(chatClient, promptText, schema, schemaName);
    }

    /**
     * 调用 AI 并解析 JSON 响应为指定类型（带自动重试）
     * Schema 名称自动使用类的简单名称，Schema 通过 @SchemaField 注解自动生成
     *
     * @param chatClient    ChatClient 实例
     * @param prompt        提示词
     * @param responseClass 响应类的 Class 对象（需标注 @SchemaField 注解）
     * @return 解析后的实体对象
     * @throws IllegalStateException 当重试次数用尽仍无法解析时抛出
     */
    public static <T> T callAndParse(
            ChatClient chatClient,
            String prompt,
            Class<T> responseClass) {
        // 复用拆分提示词版本，将完整提示词作为 userPrompt
        return callAndParse(chatClient, null, prompt, responseClass);
    }

    // ==================== 拆分提示词方法（前缀缓存优化） ====================

    /**
     * 使用流式请求调用大模型，收集完整响应（拆分提示词版本）
     * 将提示词拆分为 systemPrompt（固定部分）和 userPrompt（动态部分）
     * 利用大模型服务商的前缀缓存机制，降低 Token 消耗和延迟
     *
     * @param chatClient   ChatClient 实例
     * @param systemPrompt 系统提示词（固定部分，可被缓存）
     * @param userPrompt   用户提示词（动态部分）
     * @return 完整的响应文本
     */
    public static String callStreamAndCollect(
            ChatClient chatClient,
            String systemPrompt,
            String userPrompt) {
        return callStreamAndCollect(chatClient, systemPrompt, userPrompt, DEFAULT_OPTIONS);
    }

    /**
     * 使用流式请求调用大模型，收集完整响应（拆分提示词 + 自定义选项）
     *
     * @param chatClient   ChatClient 实例
     * @param systemPrompt 系统提示词（固定部分）
     * @param userPrompt   用户提示词（动态部分）
     * @param options      自定义的 DashScope 选项
     * @return 完整的响应文本
     */
    public static String callStreamAndCollect(
            ChatClient chatClient,
            String systemPrompt,
            String userPrompt,
            DashScopeChatOptions options) {
        Flux<String> streamResponse = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(options)
                .stream()
                .content();

        return streamResponse
                .collectList()
                .map(chunks -> String.join("", chunks))
                .block();
    }

    /**
     * 使用流式请求调用大模型，收集完整响应（拆分提示词 + JSON Schema 约束）
     * 强制大模型输出符合指定 Schema 的 JSON 格式
     *
     * @param chatClient   ChatClient 实例
     * @param systemPrompt 系统提示词（固定部分）
     * @param userPrompt   用户提示词（动态部分）
     * @param schema       JSON Schema 定义
     * @param schemaName   Schema 名称（用于日志和调试）
     * @return 完整的 JSON 响应文本
     */
    public static String callStreamAndCollectWithSchema(
            ChatClient chatClient,
            String systemPrompt,
            String userPrompt,
            Map<String, Object> schema,
            String schemaName) {

        log.debug("使用 JSON Schema 调用大模型（拆分提示词）: schemaName={}", schemaName);

        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .multiModel(true)
                .incrementalOutput(true)
                .responseFormat(DashScopeResponseFormat.builder()
                        .type(DashScopeResponseFormat.Type.JSON_SCHEMA)
                        .jsonScheme(DashScopeResponseFormat.JsonSchemaConfig.builder()
                                .name(schemaName)
                                .strict(true)
                                .schema(schema)
                                .build())
                        .build())
                .build();

        Flux<String> streamResponse = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(options)
                .stream()
                .content();

        return streamResponse
                .collectList()
                .map(chunks -> String.join("", chunks))
                .block();
    }

    /**
     * 调用 AI 并解析 JSON 响应为指定类型（拆分提示词版本，带自动重试）
     * Schema 名称自动使用类的简单名称，Schema 通过 @SchemaField 注解自动生成
     * 推荐使用此方法以利用前缀缓存优化
     *
     * @param chatClient   ChatClient 实例
     * @param systemPrompt 系统提示词（固定部分，可被缓存），可为 null
     * @param userPrompt   用户提示词（动态部分）
     * @param responseClass 响应类的 Class 对象（需标注 @SchemaField 注解）
     * @return 解析后的实体对象
     * @throws IllegalStateException 当重试次数用尽仍无法解析时抛出
     */
    public static <T> T callAndParse(
            ChatClient chatClient,
            String systemPrompt,
            String userPrompt,
            Class<T> responseClass) {

        String schemaName = responseClass.getSimpleName();
        Map<String, Object> schema = SchemaGenerator.fromClass(responseClass);

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                String response = callWithSchemaInternal(chatClient, systemPrompt, userPrompt, schema, schemaName);
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

    /**
     * 内部方法：带 JSON Schema 调用 AI
     * 根据 systemPrompt 是否为空选择调用方式
     */
    private static String callWithSchemaInternal(
            ChatClient chatClient,
            String systemPrompt,
            String userPrompt,
            Map<String, Object> schema,
            String schemaName) {
        // 如果 systemPrompt 为空，使用单提示词版本的 Schema 方法
        if (systemPrompt == null || systemPrompt.isBlank()) {
            return callStreamAndCollectWithSchema(chatClient, userPrompt, schema, schemaName);
        }
        return callStreamAndCollectWithSchema(chatClient, systemPrompt, userPrompt, schema, schemaName);
    }

    // ==================== 提示词处理工具方法 ====================

    /**
     * 替换提示词模板中的变量占位符
     * 占位符格式：{variableName}
     *
     * @param template 提示词模板
     * @param variables 变量键值对
     * @return 替换后的提示词
     */
    public static String renderTemplate(String template, Map<String, String> variables) {
        if (template == null || variables == null) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
