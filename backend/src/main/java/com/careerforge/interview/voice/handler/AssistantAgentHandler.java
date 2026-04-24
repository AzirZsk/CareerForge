package com.careerforge.interview.voice.handler;

import com.careerforge.common.config.prompt.PromptConfig;
import com.careerforge.common.config.prompt.VoiceInterviewPromptProperties;
import com.careerforge.common.enums.AssistType;
import com.careerforge.interview.voice.dto.AssistSSEEvent;
import com.careerforge.interview.voice.dto.SessionState;
import com.careerforge.interview.voice.gateway.InterviewVoiceGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * AI 助手 Agent 处理器
 * 处理面试过程中的快捷求助功能，仅返回文字回复
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssistantAgentHandler {

    private final ChatClient chatClient;
    private final VoiceInterviewPromptProperties voicePromptProperties;

    @Autowired
    @Lazy
    private InterviewVoiceGateway voiceGateway;

    /**
     * 处理快捷求助
     * 订阅 LLM 流式输出，文本增量通过 consumer 推送
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
        String systemPrompt = buildSystemPrompt(assistType, currentQuestion, contextBlock);
        String userPrompt = buildUserPrompt(assistType, currentQuestion, userQuestion, candidateDraft);
        // 调用 LLM 流式生成
        chatClient
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .stream()
                .content()
                .subscribe(
                        delta -> {
                            // 发送文本增量事件
                            eventConsumer.accept(AssistSSEEvent.text(
                                    AssistSSEEvent.TextEventData.builder()
                                            .content(delta)
                                            .isDelta(true)
                                            .build()
                            ));
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
                            // 发送完成事件
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
     * 构建系统提示词
     */
    private String buildSystemPrompt(AssistType assistType, String currentQuestion, String contextBlock) {
        VoiceInterviewPromptProperties.AssistantPromptConfig config = voicePromptProperties.getAssistant();
        if (assistType == null) {
            return "你是一位友好的技术面试助手。";
        }
        PromptConfig promptConfig = resolvePromptConfig(config, assistType);
        return promptConfig.getSystemPrompt()
                .replace("{currentQuestion}", currentQuestion)
                .replace("{conversationHistory}", contextBlock);
    }

    /**
     * 构建用户提示词
     */
    private String buildUserPrompt(
            AssistType assistType,
            String currentQuestion,
            String userQuestion,
            String candidateDraft) {

        VoiceInterviewPromptProperties.AssistantPromptConfig config = voicePromptProperties.getAssistant();
        if (assistType == null) {
            return "请告诉我你需要什么帮助。";
        }
        PromptConfig promptConfig = resolvePromptConfig(config, assistType);
        return promptConfig.getUserPromptTemplate()
                .replace("{currentQuestion}", currentQuestion)
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
