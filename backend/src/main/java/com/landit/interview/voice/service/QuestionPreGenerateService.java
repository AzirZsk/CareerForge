package com.landit.interview.voice.service;

import com.alibaba.fastjson2.JSON;
import com.landit.common.config.AIPromptProperties;
import com.landit.interview.entity.InterviewSession;
import com.landit.interview.service.InterviewSessionService;
import com.landit.interview.voice.dto.PreGenerateContext;
import com.landit.interview.voice.dto.PreGeneratedQuestion;
import com.landit.interview.voice.gateway.impl.InterviewVoiceGatewayImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 面试问题预生成服务
 * 在面试开始前一次性生成所有问题，保存到数据库
 * 面试过程中也可以生成追问（基于已有问题）
 *
 * @author Azir
 */
@Slf4j
@Service
public class QuestionPreGenerateService {

    private final ChatClient.Builder chatClientBuilder;
    private final AIPromptProperties aiPromptProperties;
    private final InterviewSessionService interviewSessionService;

    @Autowired
    @Lazy
    private InterviewVoiceGatewayImpl voiceGateway;

    public QuestionPreGenerateService(
            ChatClient.Builder chatClientBuilder,
            AIPromptProperties aiPromptProperties,
            InterviewSessionService interviewSessionService) {
        this.chatClientBuilder = chatClientBuilder;
        this.aiPromptProperties = aiPromptProperties;
        this.interviewSessionService = interviewSessionService;
    }

    /**
     * 预生成问题缓存：sessionId -> questionIndex -> PreGeneratedQuestion
     * 用于快速访问，实际数据以数据库为准
     */
    private final Map<String, Map<Integer, PreGeneratedQuestion>> preGeneratedCache = new ConcurrentHashMap<>();

    /**
     * 一次性预生成所有问题（同步）
     * 在创建会话时调用，阻塞等待所有问题生成完成
     *
     * @param sessionId 会话ID
     * @param context   预生成上下文
     * @return 生成的问题数量
     */
    public int preGenerateAllQuestions(String sessionId, PreGenerateContext context) {
        int totalQuestions = context.getTotalQuestions();
        log.info("[PreGenerate] 开始预生成所有问题, sessionId={}, total={}", sessionId, totalQuestions);

        List<PreGeneratedQuestion> questions = new ArrayList<>();

        // 获取面试官风格配置
        AIPromptProperties.InterviewerStyleConfig styleConfig = aiPromptProperties.getVoice().getByStyle(context.getInterviewerStyle());
        String systemPrompt = styleConfig.getSystemPrompt();

        // 循环生成所有问题
        for (int i = 0; i < totalQuestions; i++) {
            // 构建提示词
            String userPrompt = styleConfig.getQuestionPromptTemplate()
                    .replace("{position}", context.getPosition())
                    .replace("{questionNumber}", String.valueOf(i + 1))
                    .replace("{totalQuestions}", String.valueOf(totalQuestions))
                    .replace("{elapsedSeconds}", "0")
                    .replace("{jdRequirements}", formatJDRequirements(context.getJdContent()))
                    .replace("{resumeSummary}", formatResumeSummary(context.getResumeContent()))
                    .replace("{askedQuestions}", "暂无已提问的问题")
                    .replace("{conversationSummary}", "");

            // 调用 LLM 生成问题文本
            try {
                String questionText = generateQuestionText(systemPrompt, userPrompt);
                questions.add(PreGeneratedQuestion.builder()
                        .questionIndex(i)
                        .text(questionText)
                        .used(false)
                        .build());
                log.debug("[PreGenerate] 生成问题 {}/{}, text={}", i + 1, totalQuestions, questionText);
            } catch (Exception e) {
                log.error("[PreGenerate] 生成问题失败, index={}", i, e);
                throw new RuntimeException("预生成问题失败: " + e.getMessage(), e);
            }
        }

        // 序列化为 JSON 并保存到 session 表
        String json = JSON.toJSONString(questions);
        interviewSessionService.updatePreGeneratedQuestions(sessionId, json);
        log.info("[PreGenerate] 已保存到数据库, sessionId={}, count={}", sessionId, questions.size());

        // 同时缓存到内存（用于快速访问）
        Map<Integer, PreGeneratedQuestion> cache = questions.stream()
                .collect(Collectors.toMap(
                        PreGeneratedQuestion::getQuestionIndex,
                        Function.identity()
                ));
        preGeneratedCache.put(sessionId, cache);

        log.info("[PreGenerate] 预生成完成, sessionId={}, count={}", sessionId, questions.size());
        return questions.size();
    }

    /**
     * 生成追问（实时调用）
     * 当预生成问题不符合当前对话语境时，动态生成追问
     *
     * @param sessionId          会话ID
     * @param lastQuestion       上一个问题
     * @param candidateAnswer    候选人回答
     * @param conversationHistory 最近对话摘要
     * @return 追问文本
     */
    public String generateFollowUpQuestion(
            String sessionId,
            String lastQuestion,
            String candidateAnswer,
            String conversationHistory) {
        log.info("[PreGenerate] 生成追问, sessionId={}", sessionId);

        // 获取会话状态以获取面试官风格
        InterviewVoiceGatewayImpl.SessionState state = voiceGateway.getInternalState(sessionId);
        if (state == null) {
            log.warn("[PreGenerate] 会话不存在, sessionId={}", sessionId);
            return "不好意思，我们换个话题吧。";
        }

        // 获取面试官风格配置
        AIPromptProperties.InterviewerStyleConfig styleConfig = aiPromptProperties.getVoice().getByStyle(state.getInterviewerStyle());

        // 构建追问提示词
        String systemPrompt = styleConfig.getSystemPrompt();
        String userPrompt = buildFollowUpPrompt(
                styleConfig.getFollowUpPromptTemplate(),
                lastQuestion,
                candidateAnswer,
                conversationHistory
        );

        // 调用 LLM 生成追问
        try {
            String followUp = generateQuestionText(systemPrompt, userPrompt);
            log.info("[PreGenerate] 追问生成完成, sessionId={}, text={}", sessionId, followUp);
            return followUp;
        } catch (Exception e) {
            log.error("[PreGenerate] 追问生成失败, sessionId={}", sessionId, e);
            return "能再详细说一下吗？";
        }
    }

    /**
     * 获取缓存的预生成问题
     * 优先从内存获取，未命中则从 session 表的 JSON 字段加载
     *
     * @param sessionId     会话ID
     * @param questionIndex 问题序号
     * @return 预生成问题（如果存在且未使用）
     */
    public Optional<PreGeneratedQuestion> getCachedQuestion(String sessionId, int questionIndex) {
        // 1. 先查内存缓存
        Map<Integer, PreGeneratedQuestion> sessionCache = preGeneratedCache.get(sessionId);
        if (sessionCache != null) {
            PreGeneratedQuestion question = sessionCache.get(questionIndex);
            if (question != null && !question.isUsed()) {
                log.debug("[PreGenerate] 从内存获取问题, sessionId={}, index={}", sessionId, questionIndex);
                return Optional.of(question);
            }
        }

        // 2. 内存未命中，从 session 表加载
        InterviewSession session = interviewSessionService.getById(sessionId);
        if (session != null && session.getPreGeneratedQuestions() != null) {
            try {
                List<PreGeneratedQuestion> questions = JSON.parseArray(
                        session.getPreGeneratedQuestions(),
                        PreGeneratedQuestion.class
                );

                // 加载到内存缓存
                Map<Integer, PreGeneratedQuestion> cache = questions.stream()
                        .collect(Collectors.toMap(
                                PreGeneratedQuestion::getQuestionIndex,
                                Function.identity()
                        ));
                preGeneratedCache.put(sessionId, cache);

                PreGeneratedQuestion question = cache.get(questionIndex);
                if (question != null && !question.isUsed()) {
                    log.debug("[PreGenerate] 从数据库加载问题, sessionId={}, index={}", sessionId, questionIndex);
                    return Optional.of(question);
                }
            } catch (Exception e) {
                log.error("[PreGenerate] 解析预生成问题失败, sessionId={}", sessionId, e);
            }
        }

        log.debug("[PreGenerate] 未找到缓存问题, sessionId={}, index={}", sessionId, questionIndex);
        return Optional.empty();
    }

    /**
     * 标记问题已使用（同时更新内存和数据库）
     *
     * @param sessionId     会话ID
     * @param questionIndex 问题序号
     */
    public void markQuestionUsed(String sessionId, int questionIndex) {
        log.debug("[PreGenerate] 标记问题已使用, sessionId={}, index={}", sessionId, questionIndex);

        // 1. 更新内存缓存
        Map<Integer, PreGeneratedQuestion> sessionCache = preGeneratedCache.get(sessionId);
        if (sessionCache != null) {
            PreGeneratedQuestion question = sessionCache.get(questionIndex);
            if (question != null) {
                question.setUsed(true);
            }
        }

        // 2. 同步到数据库
        InterviewSession session = interviewSessionService.getById(sessionId);
        if (session != null && session.getPreGeneratedQuestions() != null) {
            try {
                List<PreGeneratedQuestion> questions = JSON.parseArray(
                        session.getPreGeneratedQuestions(),
                        PreGeneratedQuestion.class
                );

                questions.stream()
                        .filter(q -> q.getQuestionIndex().equals(questionIndex))
                        .findFirst()
                        .ifPresent(q -> q.setUsed(true));

                String json = JSON.toJSONString(questions);
                interviewSessionService.updatePreGeneratedQuestions(sessionId, json);
                log.debug("[PreGenerate] 已同步到数据库, sessionId={}, index={}", sessionId, questionIndex);
            } catch (Exception e) {
                log.error("[PreGenerate] 同步数据库失败, sessionId={}, index={}", sessionId, questionIndex, e);
            }
        }
    }

    /**
     * 清理会话的所有缓存（内存+数据库）
     *
     * @param sessionId 会话ID
     */
    public void clearCache(String sessionId) {
        // 清理内存缓存
        preGeneratedCache.remove(sessionId);

        // 清理数据库
        InterviewSession session = interviewSessionService.getById(sessionId);
        if (session != null) {
            session.setPreGeneratedQuestions(null);
            interviewSessionService.updateById(session);
        }

        log.info("[PreGenerate] 清理缓存完成, sessionId={}", sessionId);
    }

    /**
     * 调用 LLM 生成问题文本
     */
    private String generateQuestionText(String systemPrompt, String userPrompt) {
        return chatClientBuilder.build()
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }

    /**
     * 格式化 JD 核心要求
     */
    private String formatJDRequirements(String jdContent) {
        if (jdContent == null || jdContent.isBlank()) {
            return "暂无 JD 信息";
        }
        if (jdContent.length() > 1000) {
            return jdContent.substring(0, 1000) + "\n...(内容过长已截断)";
        }
        return jdContent;
    }

    /**
     * 格式化简历摘要
     */
    private String formatResumeSummary(String resumeContent) {
        if (resumeContent == null || resumeContent.isBlank()) {
            return "暂无简历信息";
        }
        if (resumeContent.length() > 1500) {
            return resumeContent.substring(0, 1500) + "\n...(内容过长已截断)";
        }
        return resumeContent;
    }

    /**
     * 构建追问提示词
     */
    private String buildFollowUpPrompt(
            String template,
            String lastQuestion,
            String candidateAnswer,
            String conversationHistory) {
        if (template == null || template.isBlank()) {
            // 默认追问提示词
            template = """
                    <last_question>
                    {lastQuestion}
                    </last_question>

                    <candidate_answer>
                    {candidateAnswer}
                    </candidate_answer>

                    <conversation_history>
                    {conversationHistory}
                    </conversation_history>

                    请根据候选人的回答，生成一个简短的追问（30字以内）。
                    """;
        }

        return template
                .replace("{lastQuestion}", lastQuestion != null ? lastQuestion : "")
                .replace("{candidateAnswer}", candidateAnswer != null ? candidateAnswer : "")
                .replace("{conversationHistory}", conversationHistory != null ? conversationHistory : "暂无对话历史");
    }
}
