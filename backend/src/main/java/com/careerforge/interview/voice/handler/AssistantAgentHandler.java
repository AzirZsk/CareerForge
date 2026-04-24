package com.careerforge.interview.voice.handler;

import com.careerforge.common.config.prompt.PromptConfig;
import com.careerforge.common.config.prompt.VoiceInterviewPromptProperties;
import com.careerforge.common.enums.AssistType;
import com.careerforge.interview.voice.dto.AssistSSEEvent;
import com.careerforge.interview.voice.dto.SessionState;
import com.careerforge.interview.voice.gateway.InterviewVoiceGateway;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 助手 Agent 处理器
 * 处理面试过程中的快捷求助功能，返回结构化 JSON 数据
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssistantAgentHandler {

    private final ChatClient chatClient;
    private final VoiceInterviewPromptProperties voicePromptProperties;
    private final ObjectMapper objectMapper;

    @Autowired
    @Lazy
    private InterviewVoiceGateway voiceGateway;

    /**
     * AI 返回文本中提取 JSON 的正则
     */
    private static final Pattern JSON_PATTERN = Pattern.compile("\\{[\\s\\S]*\\}", Pattern.DOTALL);

    /**
     * 处理快捷求助
     * 收集 LLM 完整输出，解析 JSON 后推送 structured 事件
     *
     * @param sessionId      会话 ID
     * @param assistType     求助类型
     * @param userQuestion   用户问题（自由提问时使用）
     * @param candidateDraft 候选人草稿（润色时使用）
     * @param eventConsumer  SSE 事件消费者
     */
    public void handleAssist(
            String sessionId,
            AssistType assistType,
            String userQuestion,
            String candidateDraft,
            Consumer<AssistSSEEvent> eventConsumer) {

        log.info("[AssistantAgent] 处理求助请求, sessionId={}, type={}", sessionId, assistType);
        SessionState sessionState = voiceGateway.getInternalState(sessionId);
        // 获取当前问题文本
        String currentQuestion = sessionState != null && sessionState.getLastQuestion() != null
                ? sessionState.getLastQuestion() : "未知";
        // 获取对话历史
        String conversationHistory = voiceGateway.getConversationSummary(sessionId);
        String contextBlock = buildConversationContext(conversationHistory);
        // 构建提示词
        String systemPrompt = buildSystemPrompt(assistType);
        String userPrompt = buildUserPrompt(assistType, currentQuestion, contextBlock, userQuestion, candidateDraft);
        // 收集完整文本
        StringBuilder fullText = new StringBuilder();
        // 调用 LLM 流式生成
        chatClient
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .stream()
                .content()
                .subscribe(
                        delta -> {
                            // 只收集文本，不推送 text 事件
                            fullText.append(delta);
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
                            // AI 完成：解析 JSON 并推送 structured 事件
                            String rawText = fullText.toString().trim();
                            log.info("[AssistantAgent] AI输出完成, length={}, preview={}", rawText.length(),
                                    rawText.length() > 200 ? rawText.substring(0, 200) : rawText);
                            Map<String, Object> structuredContent = parseJsonResponse(rawText);
                            eventConsumer.accept(AssistSSEEvent.structured(
                                    AssistSSEEvent.StructuredEventData.builder()
                                            .assistType(assistType.getCode())
                                            .content(structuredContent)
                                            .build()
                            ));
                            // 推送 done 事件
                            eventConsumer.accept(AssistSSEEvent.done(
                                    AssistSSEEvent.DoneEventData.builder()
                                            .assistRemaining(sessionState != null ? sessionState.getAssistRemaining() : 0)
                                            .totalDurationMs(0)
                                            .build()
                            ));
                        }
                );
    }

    /**
     * 解析 AI 返回的 JSON 文本
     * 含降级逻辑：先直接解析，失败则正则提取，再失败则包装为 fallbackText
     */
    private Map<String, Object> parseJsonResponse(String text) {
        if (text == null || text.isBlank()) {
            return Map.of("fallbackText", "AI 未返回有效内容");
        }
        // 第一步：直接解析
        try {
            return objectMapper.readValue(text, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.debug("[AssistantAgent] 直接JSON解析失败, 尝试正则提取");
        }
        // 第二步：正则提取 JSON 部分
        Matcher matcher = JSON_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return objectMapper.readValue(matcher.group(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.debug("[AssistantAgent] 正则提取后JSON解析失败");
            }
        }
        // 第三步：降级为纯文本
        log.warn("[AssistantAgent] JSON解析全部失败, 降级为纯文本, 原文: {}", text.length() > 100 ? text.substring(0, 100) : text);
        return Map.of("fallbackText", text);
    }

    /**
     * 构建对话上下文文本块
     * 截取最近2000字符，避免过长
     */
    private String buildConversationContext(String conversationHistory) {
        if (conversationHistory == null || conversationHistory.isBlank()) {
            return "";
        }
        String recent = conversationHistory.length() > 2000
                ? conversationHistory.substring(conversationHistory.length() - 2000)
                : conversationHistory;
        return "以下是面试官与候选人最近的对话：\n" + recent;
    }

    /**
     * 构建系统提示词（纯静态内容，不含动态变量，支持前缀缓存）
     */
    private String buildSystemPrompt(AssistType assistType) {
        VoiceInterviewPromptProperties.AssistantPromptConfig config = voicePromptProperties.getAssistant();
        if (assistType == null) {
            return "你是一位友好的技术面试助手。";
        }
        PromptConfig promptConfig = resolvePromptConfig(config, assistType);
        return promptConfig.getSystemPrompt();
    }

    /**
     * 构建用户提示词（所有动态变量统一在此替换）
     */
    private String buildUserPrompt(
            AssistType assistType,
            String currentQuestion,
            String conversationHistory,
            String userQuestion,
            String candidateDraft) {

        VoiceInterviewPromptProperties.AssistantPromptConfig config = voicePromptProperties.getAssistant();
        if (assistType == null) {
            return "请告诉我你需要什么帮助。";
        }
        PromptConfig promptConfig = resolvePromptConfig(config, assistType);
        return promptConfig.getUserPromptTemplate()
                .replace("{currentQuestion}", currentQuestion)
                .replace("{conversationHistory}", conversationHistory != null ? conversationHistory : "")
                .replace("{candidateDraft}", candidateDraft != null ? candidateDraft : "请告诉我你目前的回答内容。")
                .replace("{userQuestion}", userQuestion != null ? userQuestion : "请问你有什么问题？");
    }

    /**
     * 根据求助类型解析对应的 PromptConfig
     */
    private PromptConfig resolvePromptConfig(VoiceInterviewPromptProperties.AssistantPromptConfig config, AssistType assistType) {
        return switch (assistType) {
            case GIVE_HINTS -> config.getHints();
            case EXPLAIN_CONCEPT -> config.getExplain();
            case POLISH_ANSWER -> config.getPolish();
            case FREE_QUESTION -> config.getFreeQuestion();
        };
    }
}
