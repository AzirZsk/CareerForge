package com.landit.interview.voice.handler.impl;

import com.landit.common.config.VoiceProperties;
import com.landit.interview.voice.dto.*;
import com.landit.interview.voice.gateway.impl.InterviewVoiceGatewayImpl;
import com.landit.interview.voice.handler.AssistantAgentHandler;
import com.landit.interview.voice.service.TTSService;
import com.landit.interview.voice.service.VoiceServiceFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 助手 Agent 处理器实现
 * 处理面试过程中的快捷求助功能
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssistantAgentHandlerImpl implements AssistantAgentHandler {

    private final VoiceServiceFactory voiceServiceFactory;
    private final VoiceProperties voiceProperties;
    private final ChatClient.Builder chatClientBuilder;

    @Autowired
    @Lazy
    private InterviewVoiceGatewayImpl voiceGateway;

    // 会话上下文
    private final ConcurrentHashMap<String, AssistContext> contexts = new ConcurrentHashMap<>();

    @Override
    public Flux<AssistSSEEvent> handleAssist(
            String sessionId,
            String assistType,
            String userQuestion,
            String candidateDraft) {

        log.info("[AssistantAgent] Handling assist, sessionId={}, type={}", sessionId, assistType);

        // 获取上下文
        AssistContext context = contexts.computeIfAbsent(sessionId, k -> new AssistContext());
        InterviewVoiceGatewayImpl.SessionState sessionState = voiceGateway.getInternalState(sessionId);

        // 构建提示词
        String systemPrompt = buildSystemPrompt(assistType, sessionState);
        String userPrompt = buildUserPrompt(assistType, userQuestion, candidateDraft, sessionState);

        // 获取 TTS 服务
        TTSService ttsService = voiceServiceFactory.getTTSService();
        TTSConfig ttsConfig = buildAssistantTTSConfig();

        // 文本缓冲区
        StringBuilder textBuffer = new StringBuilder();

        // 调用 LLM 流式生成
        return chatClientBuilder.build()
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .stream()
                .content()
                .flatMap(textDelta -> {
                    textBuffer.append(textDelta);

                    // 发送文本事件
                    AssistSSEEvent textEvent = AssistSSEEvent.text(
                            AssistSSEEvent.TextEventData.builder()
                                    .content(textDelta)
                                    .isDelta(true)
                                    .build()
                    );

                    // 如果句子结束，同时触发 TTS
                    if (isSentenceEnd(textDelta)) {
                        String sentence = textBuffer.toString();
                        // 保留最后一个字符继续缓冲
                    textBuffer.setLength(0);

                        // 发送 TTS 音频
                        Flux<AssistSSEEvent> audioFlux = ttsService.streamSynthesize(sentence, ttsConfig)
                                .map(audioData -> AssistSSEEvent.audio(
                                        AssistSSEEvent.AudioEventData.builder()
                                                .audio(Base64.getEncoder().encodeToString(audioData))
                                                .format(ttsConfig.getFormat())
                                                .sampleRate(ttsConfig.getSampleRate())
                                                .build()
                                ))
                                .onErrorResume(e -> {
                                    log.error("[AssistantAgent] TTS error", e);
                                    return Flux.empty();
                                });

                        return Flux.concat(Flux.just(textEvent), audioFlux);
                    }

                    return Flux.just(textEvent);
                })
                .concatWith(Flux.defer(() -> {
                    // 处理剩余文本
                    if (textBuffer.length() > 0) {
                        String lastText = textBuffer.toString();
                        return ttsService.streamSynthesize(lastText, ttsConfig)
                                .map(audioData -> AssistSSEEvent.audio(
                                        AssistSSEEvent.AudioEventData.builder()
                                                .audio(Base64.getEncoder().encodeToString(audioData))
                                                .format(ttsConfig.getFormat())
                                                .sampleRate(ttsConfig.getSampleRate())
                                                .build()
                                ));
                    }
                    return Flux.empty();
                }))
                .concatWith(Flux.just(
                        AssistSSEEvent.done(AssistSSEEvent.DoneEventData.builder()
                                .assistRemaining(sessionState != null ? sessionState.getAssistRemaining() : 0)
                                .totalDurationMs(0)
                                .build())
                ))
                .onErrorResume(e -> {
                    log.error("[AssistantAgent] Error handling assist", e);
                    return Flux.just(AssistSSEEvent.error(
                            AssistSSEEvent.ErrorEventData.builder()
                                    .code("ASSIST_ERROR")
                                    .message(e.getMessage())
                                    .build()
                    ));
                });
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(String assistType, InterviewVoiceGatewayImpl.SessionState sessionState) {
        String currentQuestion = sessionState != null ? String.valueOf(sessionState.getCurrentQuestion()) : "未知";

        return switch (assistType) {
            case "GIVE_HINTS" -> """
                你是一位耐心的技术面试辅导助手。

                当前面试问题：%s

                请给出回答思路提示，要求：
                1. 不要直接给出答案
                2. 提供思考框架和方法论
                3. 列出关键知识点
                4. 给出回答结构建议
                5. 语言简洁，控制在 100 字以内
                """.formatted(currentQuestion);

            case "EXPLAIN_CONCEPT" -> """
                你是一位技术概念讲解专家。

                请用简洁易懂的语言解释技术概念，要求：
                1. 先给出概念定义
                2. 用类比帮助理解
                3. 说明应用场景
                4. 给出代码示例（如果适用）
                5. 控制在 150 字以内
                """;

            case "POLISH_ANSWER" -> """
                你是一位专业的面试回答润色专家。

                请润色候选人的回答，要求：
                1. 保持原意，提升表达
                2. 使语言更专业流畅
                3. 补充关键细节
                4. 控制篇幅增长在 30% 以内
                5. 指出改进点
                """;

            case "FREE_QUESTION" -> """
                你是一位技术面试顾问。

                请回答候选人的问题，要求：
                1. 直接回答问题
                2. 提供具体建议
                3. 如果是技术问题，给出代码示例
                4. 控制在 200 字以内
                """;

            default -> "你是一位友好的技术面试助手。";
        };
    }

    /**
     * 构建用户提示词
     */
    private String buildUserPrompt(
            String assistType,
            String userQuestion,
            String candidateDraft,
            InterviewVoiceGatewayImpl.SessionState sessionState) {

        String currentQuestion = sessionState != null ? String.valueOf(sessionState.getCurrentQuestion()) : "未知";

        return switch (assistType) {
            case "GIVE_HINTS" -> "请为问题 \"" + currentQuestion + "\" 给我思路提示。";
            case "EXPLAIN_CONCEPT" -> userQuestion != null ? userQuestion : "请解释这个问题涉及的核心概念。";
            case "POLISH_ANSWER" -> candidateDraft != null ?
                    "请润色我的回答：" + candidateDraft : "请告诉我你目前的回答内容。";
            case "FREE_QUESTION" -> userQuestion != null ? userQuestion : "请问你有什么问题？";
            default -> "请告诉我你需要什么帮助。";
        };
    }

    /**
     * 构建助手 TTS 配置
     */
    private TTSConfig buildAssistantTTSConfig() {
        VoiceProperties.AliyunConfig.TTSConfig ttsConfig = voiceProperties.getAliyun().getTts();
        String voice = voiceProperties.getAliyun().getVoices().getAssistant();
        return TTSConfig.builder()
                .model(ttsConfig.getModel())
                .voice(voice)
                .format(ttsConfig.getFormat())
                .sampleRate(ttsConfig.getSampleRate())
                // 助手语速稍快
                .speechRate(1.1)
                .volume(0.8)
                // 音调略高更亲切
                .pitch(0.1)
                .build();
    }

    /**
     * 判断是否句子结束
     */
    private boolean isSentenceEnd(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        char lastChar = text.charAt(text.length() - 1);
        return "。！？.!;；\n".indexOf(lastChar) >= 0;
    }

    /**
     * 助手上下文
     */
    @lombok.Data
    private static class AssistContext {
        private int assistCount = 0;
    }
}
