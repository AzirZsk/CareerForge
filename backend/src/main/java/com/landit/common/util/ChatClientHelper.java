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
}
