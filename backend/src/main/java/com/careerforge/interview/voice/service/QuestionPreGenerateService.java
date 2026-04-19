package com.careerforge.interview.voice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.careerforge.common.config.AIPromptProperties;
import com.careerforge.common.util.ChatClientHelper;
import com.careerforge.interview.entity.InterviewSession;
import com.careerforge.interview.service.InterviewSessionService;
import com.careerforge.interview.voice.dto.BatchQuestionsResponse;
import com.careerforge.interview.voice.dto.PreGenerateContext;
import com.careerforge.interview.voice.dto.PreGeneratedQuestion;
import com.careerforge.interview.voice.dto.SessionState;
import com.careerforge.interview.voice.gateway.InterviewVoiceGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 面试问题预生成服务
 * 负责在会话创建时预生成所有面试问题，缓存到内存和数据库
 *
 * @author Azir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionPreGenerateService {

    private final ChatClient chatClient;
    private final AIPromptProperties aiPromptProperties;
    private final InterviewSessionService interviewSessionService;
    private final ObjectMapper objectMapper;

    @Autowired
    @Lazy
    private InterviewVoiceGateway voiceGateway;

    /**
     * 预生成问题缓存：sessionId -> questionIndex -> PreGeneratedQuestion
     * 用于快速访问，实际数据以数据库为准
     */
    private final Map<String, Map<Integer, PreGeneratedQuestion>> preGeneratedCache = new ConcurrentHashMap<>();

    /**
     * 一次性预生成所有问题（同步）
     *
     * @param sessionId 会话ID
     * @param context   预生成上下文
     * @return 生成的问题数量
     */
    public int preGenerateAllQuestions(String sessionId, PreGenerateContext context) {
        int totalQuestions = context.getTotalQuestions();
        log.info("[PreGenerate] 开始预生成所有问题, sessionId={}, total={}", sessionId, totalQuestions);

        // 获取预生成提示词配置
        AIPromptProperties.PromptConfig preGenerateConfig = aiPromptProperties.getQuestionPreGenerate();
        String systemPrompt = preGenerateConfig.getSystemPrompt();

        // 构建批量生成提示词
        String userPrompt = buildBatchPreGeneratePrompt(preGenerateConfig.getUserPromptTemplate(), context);

        // 调用 LLM 并用实体接收结构化响应
        BatchQuestionsResponse response;
        try {
            response = ChatClientHelper.callAndParse(chatClient, systemPrompt, userPrompt, BatchQuestionsResponse.class);
            log.info("[PreGenerate] LLM 返回结果, sessionId={}, questionsCount={}", sessionId,
                    response.getQuestions() != null ? response.getQuestions().size() : 0);
        } catch (Exception e) {
            log.error("[PreGenerate] 批量生成问题失败, sessionId={}", sessionId, e);
            throw new RuntimeException("批量生成问题失败: " + e.getMessage(), e);
        }

        // 转换为 PreGeneratedQuestion 列表
        List<BatchQuestionsResponse.QuestionItem> questionItems = response.getQuestions();
        if (questionItems == null || questionItems.isEmpty()) {
            log.error("[PreGenerate] LLM 返回的问题列表为空, sessionId={}", sessionId);
            throw new RuntimeException("批量生成问题失败: 返回问题列表为空");
        }

        if (questionItems.size() != totalQuestions) {
            log.warn("[PreGenerate] 生成的问题数量不匹配, 期望={}, 实际={}", totalQuestions, questionItems.size());
        }

        List<PreGeneratedQuestion> questions = questionItems.stream()
                .map(item -> item.getText() != null ? item.getText().trim() : "")
                .filter(text -> !text.isEmpty())
                .map(text -> PreGeneratedQuestion.builder()
                        .questionIndex(0)
                        .text(text)
                        .used(false)
                        .build())
                .toList();

        // 重新编号
        for (int i = 0; i < questions.size(); i++) {
            questions.get(i).setQuestionIndex(i);
            log.debug("[PreGenerate] 问题 {}, text={}", i + 1, questions.get(i).getText());
        }

        // 序列化为 JSON 并保存到 session 表
        String json;
        try {
            json = objectMapper.writeValueAsString(questions);
        } catch (JsonProcessingException e) {
            log.error("[PreGenerate] 序列化问题失败", e);
            throw new RuntimeException("序列化问题失败: " + e.getMessage(), e);
        }
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
        SessionState state = voiceGateway.getInternalState(sessionId);
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

        // 调用 LLM 生成追问（纯文本调用，无需结构化）
        try {
            String followUp = chatClient
                    .prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .options(OpenAiChatOptions.builder()
                            .extraBody(Map.of("enable_thinking", false))
                            .build())
                    .call()
                    .content();
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
                List<PreGeneratedQuestion> questions = objectMapper.readValue(
                        session.getPreGeneratedQuestions(),
                        new TypeReference<List<PreGeneratedQuestion>>() {}
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
                List<PreGeneratedQuestion> questions = objectMapper.readValue(
                        session.getPreGeneratedQuestions(),
                        new TypeReference<List<PreGeneratedQuestion>>() {}
                );

                questions.stream()
                        .filter(q -> q.getQuestionIndex().equals(questionIndex))
                        .findFirst()
                        .ifPresent(q -> q.setUsed(true));

                String json = objectMapper.writeValueAsString(questions);
                interviewSessionService.updatePreGeneratedQuestions(sessionId, json);
                log.debug("[PreGenerate] 已同步到数据库, sessionId={}, index={}", sessionId, questionIndex);
            } catch (Exception e) {
                log.error("[PreGenerate] 同步数据库失败, sessionId={}, index={}", sessionId, questionIndex, e);
            }
        }
    }

    /**
     * 从已有会话复制预生成问题到新会话
     * 重置 used 标记并重新编号，数量不匹配时返回 -1
     *
     * @param newSessionId        新会话ID
     * @param sourceQuestionsJson 源会话的问题 JSON
     * @param targetCount         目标问题数量
     * @return 复制的问题数量，数量不匹配返回 -1
     */
    public int copyPreGeneratedQuestions(String newSessionId, String sourceQuestionsJson, int targetCount) {
        try {
            List<PreGeneratedQuestion> questions = objectMapper.readValue(
                    sourceQuestionsJson,
                    new TypeReference<List<PreGeneratedQuestion>>() {}
            );
            // 源问题列表为空，无法复用
            if (questions.isEmpty()) {
                log.warn("[PreGenerate] 源问题列表为空, newSessionId={}", newSessionId);
                return -1;
            }
            // 数量不匹配，让调用方降级到重新生成
            if (questions.size() != targetCount) {
                log.warn("[PreGenerate] 历史问题数量不匹配, 期望={}, 实际={}", targetCount, questions.size());
                return -1;
            }
            // 重置 used 标记并重新编号
            List<PreGeneratedQuestion> copiedQuestions = questions.stream()
                    .map(q -> PreGeneratedQuestion.builder()
                            .questionIndex(0)
                            .text(q.getText())
                            .used(false)
                            .build())
                    .toList();
            for (int i = 0; i < copiedQuestions.size(); i++) {
                copiedQuestions.get(i).setQuestionIndex(i);
            }
            // 保存到新会话数据库
            String json = objectMapper.writeValueAsString(copiedQuestions);
            interviewSessionService.updatePreGeneratedQuestions(newSessionId, json);
            // 缓存到内存
            Map<Integer, PreGeneratedQuestion> cache = copiedQuestions.stream()
                    .collect(Collectors.toMap(
                            PreGeneratedQuestion::getQuestionIndex,
                            Function.identity()
                    ));
            preGeneratedCache.put(newSessionId, cache);
            log.info("[PreGenerate] 复制问题完成, newSessionId={}, count={}", newSessionId, copiedQuestions.size());
            return copiedQuestions.size();
        } catch (Exception e) {
            log.error("[PreGenerate] 复制问题失败, newSessionId={}", newSessionId, e);
            return -1;
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

    /**
     * 构建批量预生成问题的提示词
     * JD 和简历内容都完整输入不截断
     *
     * @param template 提示词模板
     * @param context  预生成上下文
     * @return 替换占位符后的提示词
     * @author Azir
     */
    private String buildBatchPreGeneratePrompt(String template, PreGenerateContext context) {
        // JD 和简历都完整输入，不截断
        String jdContent = context.getJdContent() != null ? context.getJdContent() : "暂无 JD 信息";
        String resumeContent = context.getResumeContent() != null ? context.getResumeContent() : "暂无简历信息";

        return template
                .replace("{position}", context.getPosition())
                .replace("{totalQuestions}", String.valueOf(context.getTotalQuestions()))
                .replace("{jdContent}", jdContent)
                .replace("{resumeContent}", resumeContent)
                .replace("{jdAnalysis}", context.getJdAnalysis() != null ? context.getJdAnalysis() : "暂无")
                ;
    }

}
