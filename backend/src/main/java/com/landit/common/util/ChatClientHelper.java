package com.landit.common.util;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

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
}
