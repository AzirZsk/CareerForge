package com.careerforge.interview.voice.handler;

import com.careerforge.common.config.VoiceProperties;
import com.careerforge.interview.voice.dto.*;
import com.careerforge.interview.voice.dto.SessionState;
import com.careerforge.interview.voice.gateway.InterviewVoiceGateway;
import com.careerforge.interview.voice.service.TTSListener;
import com.careerforge.interview.voice.service.TTSService;
import com.careerforge.interview.voice.service.VoiceServiceFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * AI 助手 Agent 处理器
 * 处理面试过程中的快捷求助功能
 * 采用回调模式，SSE 事件通过 Consumer 直接推送
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssistantAgentHandler {

    private final VoiceServiceFactory voiceServiceFactory;
    private final VoiceProperties voiceProperties;
    private final ChatClient chatClient;

    @Autowired
    @Lazy
    private InterviewVoiceGateway voiceGateway;

    private static final String SENTENCE_END_CHARS = "。！？.!;；\n";

    // 会话上下文
    private final ConcurrentHashMap<String, AssistContext> contexts = new ConcurrentHashMap<>();

    /**
     * 处理快捷求助
     * 订阅 LLM 流式输出，按句子分割后通过 TTS 合成音频，所有事件通过 consumer 推送
     *
     * @param sessionId      会话 ID
     * @param assistType     求助类型
     * @param userQuestion   用户问题（自由提问时使用）
     * @param candidateDraft 候选人草稿（润色时使用）
     * @param eventConsumer  SSE 事件消费者
     */
    public void handleAssist(
            String sessionId,
            String assistType,
            String userQuestion,
            String candidateDraft,
            Consumer<AssistSSEEvent> eventConsumer) {

        log.info("[AssistantAgent] 处理求助请求, sessionId={}, type={}", sessionId, assistType);

        // 获取上下文
        AssistContext context = contexts.computeIfAbsent(sessionId, k -> new AssistContext());
        SessionState sessionState = voiceGateway.getInternalState(sessionId);

        // 构建提示词
        String systemPrompt = buildSystemPrompt(assistType, sessionState);
        String userPrompt = buildUserPrompt(assistType, userQuestion, candidateDraft, sessionState);

        // 获取 TTS 服务
        TTSService ttsService = voiceServiceFactory.getTTSService();
        TTSConfig ttsConfig = buildAssistantTTSConfig();

        // 文本缓冲区
        StringBuilder textBuffer = new StringBuilder();

        // 调用 LLM 流式生成，按句子分割后 TTS 合成
        chatClient
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .stream()
                .content()
                .subscribe(
                        delta -> {
                            textBuffer.append(delta);
                            // 发送文本增量事件
                            eventConsumer.accept(AssistSSEEvent.text(
                                    AssistSSEEvent.TextEventData.builder()
                                            .content(delta)
                                            .isDelta(true)
                                            .build()
                            ));
                            // 句子结束时触发 TTS 合成
                            if (isSentenceEnd(delta)) {
                                String sentence = textBuffer.toString();
                                textBuffer.setLength(0);
                                // 合成音频并通过回调推送
                                ttsService.streamSynthesize(sentence, ttsConfig, new TTSListener() {
                                    @Override
                                    public void onAudio(byte[] audioData) {
                                        eventConsumer.accept(AssistSSEEvent.audio(
                                                AssistSSEEvent.AudioEventData.builder()
                                                        .audio(Base64.getEncoder().encodeToString(audioData))
                                                        .format(ttsConfig.getFormat())
                                                        .sampleRate(ttsConfig.getSampleRate())
                                                        .build()
                                        ));
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        log.error("[AssistantAgent] TTS错误", e);
                                    }

                                    @Override
                                    public void onComplete() {
                                        // 单句合成完成，无需特殊处理
                                    }
                                });
                            }
                        },
                        error -> {
                            log.error("[AssistantAgent] 处理求助出错", error);
                            eventConsumer.accept(AssistSSEEvent.error(
                                    AssistSSEEvent.ErrorEventData.builder()
                                            .code("ASSIST_ERROR")
                                            .message(error.getMessage())
                                            .build()
                            ));
                        },
                        () -> {
                            // 处理剩余文本
                            if (textBuffer.length() > 0) {
                                String lastText = textBuffer.toString();
                                textBuffer.setLength(0);
                                ttsService.streamSynthesize(lastText, ttsConfig, new TTSListener() {
                                    @Override
                                    public void onAudio(byte[] audioData) {
                                        eventConsumer.accept(AssistSSEEvent.audio(
                                                AssistSSEEvent.AudioEventData.builder()
                                                        .audio(Base64.getEncoder().encodeToString(audioData))
                                                        .format(ttsConfig.getFormat())
                                                        .sampleRate(ttsConfig.getSampleRate())
                                                        .build()
                                        ));
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        log.error("[AssistantAgent] TTS错误(剩余文本)", e);
                                    }

                                    @Override
                                    public void onComplete() {
                                        // 发送完成事件
                                        eventConsumer.accept(AssistSSEEvent.done(
                                                AssistSSEEvent.DoneEventData.builder()
                                                        .assistRemaining(sessionState != null ? sessionState.getAssistRemaining() : 0)
                                                        .totalDurationMs(0)
                                                        .build()
                                        ));
                                    }
                                });
                            } else {
                                // 没有剩余文本，直接发送完成事件
                                eventConsumer.accept(AssistSSEEvent.done(
                                        AssistSSEEvent.DoneEventData.builder()
                                                .assistRemaining(sessionState != null ? sessionState.getAssistRemaining() : 0)
                                                .totalDurationMs(0)
                                                .build()
                                ));
                            }
                        }
                );
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(String assistType, SessionState sessionState) {
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
            SessionState sessionState) {

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
                .speechRate(1.1)
                .volume(0.8)
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
        return SENTENCE_END_CHARS.indexOf(lastChar) >= 0;
    }

    /**
     * 助手上下文
     */
    @lombok.Data
    private static class AssistContext {
        private int assistCount = 0;
    }
}
