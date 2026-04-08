package com.landit.interview.voice.service.impl;

import com.landit.common.config.AIPromptProperties;
import com.landit.common.config.VoiceProperties;
import com.landit.interview.voice.dto.*;
import com.landit.interview.voice.gateway.impl.InterviewVoiceGatewayImpl;
import com.landit.interview.voice.service.QuestionPreGenerateService;
import com.landit.interview.voice.service.TTSService;
import com.landit.interview.voice.service.VoiceServiceFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 面试问题预生成服务实现
 * 在面试开始前预生成问题，实现零延迟推送
 *
 * @author Azir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionPreGenerateServiceImpl implements QuestionPreGenerateService {

    private final InterviewVoiceGatewayImpl voiceGateway;
    private final ChatClient.Builder chatClientBuilder;
    private final VoiceServiceFactory voiceServiceFactory;
    private final AIPromptProperties aiPromptProperties;
    private final VoiceProperties voiceProperties;

    /**
     * 预生成问题缓存：sessionId -> questionIndex -> PreGeneratedQuestion
     */
    private final Map<String, Map<Integer, PreGeneratedQuestion>> preGeneratedCache = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<PreGeneratedQuestion> preGenerateFirstQuestion(String sessionId, PreGenerateContext context) {
        log.info("[PreGenerate] 开始预生成第一个问题, sessionId={}, position={}", sessionId, context.getPosition());

        // 获取面试官风格配置
        AIPromptProperties.InterviewerStyleConfig styleConfig = aiPromptProperties.getVoice().getByStyle(context.getInterviewerStyle());

        // 构建提示词
        String systemPrompt = styleConfig.getSystemPrompt();
        String userPrompt = styleConfig.getQuestionPromptTemplate()
                .replace("{position}", context.getPosition())
                .replace("{questionNumber}", "1")
                .replace("{totalQuestions}", String.valueOf(context.getTotalQuestions()))
                .replace("{elapsedSeconds}", "0")
                .replace("{jdRequirements}", formatJDRequirements(context.getJdContent()))
                .replace("{resumeSummary}", formatResumeSummary(context.getResumeContent()))
                .replace("{askedQuestions}", "暂无已提问的问题")
                .replace("{conversationSummary}", "");

        // 异步生成
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Step 1: 调用 LLM 生成问题文本
                String questionText = generateQuestionText(systemPrompt, userPrompt);
                log.debug("[PreGenerate] LLM 生成完成, text={}", questionText);

                // Step 2: 调用 TTS 合成音频
                byte[] audioData = synthesizeAudio(questionText, context.getInterviewerStyle());
                log.debug("[PreGenerate] TTS 合成完成, audioSize={}", audioData.length);

                // Step 3: 构建预生成问题
                PreGeneratedQuestion question = PreGeneratedQuestion.builder()
                        .questionIndex(0)
                        .text(questionText)
                        .audioData(audioData)
                        .audioFormat("wav")
                        .sampleRate(16000)
                        .used(false)
                        .build();

                // Step 4: 缓存
                preGeneratedCache.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                        .put(0, question);

                log.info("[PreGenerate] 预生成完成, sessionId={}", sessionId);
                return question;

            } catch (Exception e) {
                log.error("[PreGenerate] 预生成失败, sessionId={}", sessionId, e);
                throw new RuntimeException("预生成失败: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public void preGenerateNextQuestion(String sessionId, int nextQuestionIndex) {
        log.info("[PreGenerate] 开始预生成下一个问题, sessionId={}, nextIndex={}", sessionId, nextQuestionIndex);

        // 获取会话状态
        InterviewVoiceGatewayImpl.SessionState state = voiceGateway.getInternalState(sessionId);
        if (state == null) {
            log.warn("[PreGenerate] 会话不存在, sessionId={}", sessionId);
            return;
        }

        // 检查是否已缓存
        Map<Integer, PreGeneratedQuestion> sessionCache = preGeneratedCache.get(sessionId);
        if (sessionCache != null && sessionCache.containsKey(nextQuestionIndex)) {
            log.debug("[PreGenerate] 问题已缓存, sessionId={}, index={}", sessionId, nextQuestionIndex);
            return;
        }

        // 获取面试官风格配置
        String style = state.getInterviewerStyle();
        AIPromptProperties.InterviewerStyleConfig styleConfig = aiPromptProperties.getVoice().getByStyle(style);

        // 构建上下文
        String position = state.getPosition();
        String jdContent = state.getJdContent();
        String resumeContent = state.getResumeContent();

        // 构建提示词
        String systemPrompt = styleConfig.getSystemPrompt();
        String userPrompt = styleConfig.getQuestionPromptTemplate()
                .replace("{position}", position != null ? position : "Java 开发工程师")
                .replace("{questionNumber}", String.valueOf(nextQuestionIndex + 1))
                .replace("{totalQuestions}", String.valueOf(state.getTotalQuestions()))
                .replace("{elapsedSeconds}", String.valueOf(state.getElapsedTime()))
                .replace("{jdRequirements}", formatJDRequirements(jdContent))
                .replace("{resumeSummary}", formatResumeSummary(resumeContent))
                .replace("{askedQuestions}", "暂无已提问的问题")
                .replace("{conversationSummary}", "");

        // 异步生成（不阻塞）
        CompletableFuture.runAsync(() -> {
            try {
                String questionText = generateQuestionText(systemPrompt, userPrompt);
                byte[] audioData = synthesizeAudio(questionText, style);

                PreGeneratedQuestion question = PreGeneratedQuestion.builder()
                        .questionIndex(nextQuestionIndex)
                        .text(questionText)
                        .audioData(audioData)
                        .audioFormat("wav")
                        .sampleRate(16000)
                        .used(false)
                        .build();

                preGeneratedCache.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                        .put(nextQuestionIndex, question);

                log.info("[PreGenerate] 预生成完成, sessionId={}, index={}", sessionId, nextQuestionIndex);

            } catch (Exception e) {
                log.error("[PreGenerate] 预生成失败, sessionId={}, index={}", sessionId, nextQuestionIndex, e);
            }
        });
    }

    @Override
    public Optional<PreGeneratedQuestion> getCachedQuestion(String sessionId, int questionIndex) {
        Map<Integer, PreGeneratedQuestion> sessionCache = preGeneratedCache.get(sessionId);
        if (sessionCache == null) {
            return Optional.empty();
        }
        PreGeneratedQuestion question = sessionCache.get(questionIndex);
        if (question == null || question.isUsed()) {
            return Optional.empty();
        }
        return Optional.of(question);
    }

    @Override
    public void markQuestionUsed(String sessionId, int questionIndex) {
        Map<Integer, PreGeneratedQuestion> sessionCache = preGeneratedCache.get(sessionId);
        if (sessionCache != null) {
            PreGeneratedQuestion question = sessionCache.get(questionIndex);
            if (question != null) {
                question.setUsed(true);
                log.debug("[PreGenerate] 标记问题已使用, sessionId={}, index={}", sessionId, questionIndex);
            }
        }
    }

    @Override
    public void clearCache(String sessionId) {
        preGeneratedCache.remove(sessionId);
        log.info("[PreGenerate] 清理缓存, sessionId={}", sessionId);
    }

    @Override
    public boolean isPreGenerationComplete(String sessionId) {
        Map<Integer, PreGeneratedQuestion> sessionCache = preGeneratedCache.get(sessionId);
        if (sessionCache == null) {
            return false;
        }
        PreGeneratedQuestion firstQuestion = sessionCache.get(0);
        return firstQuestion != null && !firstQuestion.isUsed();
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
     * 调用 TTS 合成音频
     */
    private byte[] synthesizeAudio(String text, String interviewerStyle) {
        TTSService ttsService = voiceServiceFactory.getTTSService();
        TTSConfig ttsConfig = buildInterviewerTTSConfig(interviewerStyle);

        ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();

        // 使用流式合成，收集所有音频块
        ttsService.streamSynthesize(text, ttsConfig)
                .doOnNext(chunk -> {
                    try {
                        audioBuffer.write(chunk);
                    } catch (Exception e) {
                        log.error("[PreGenerate] 写入音频缓冲失败", e);
                    }
                })
                .blockLast();

        return audioBuffer.toByteArray();
    }

    /**
     * 构建面试官 TTS 配置
     */
    private TTSConfig buildInterviewerTTSConfig(String interviewerStyle) {
        VoiceProperties.AliyunConfig.TTSConfig ttsConfig = voiceProperties.getAliyun().getTts();
        String voice = voiceProperties.getAliyun().getVoices().getInterviewer();
        return TTSConfig.builder()
                .model(ttsConfig.getModel())
                .voice(voice)
                .format(ttsConfig.getFormat())
                .sampleRate(ttsConfig.getSampleRate())
                .speechRate(1.0)
                .volume(0.8)
                .pitch(0.0)
                .build();
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
}
