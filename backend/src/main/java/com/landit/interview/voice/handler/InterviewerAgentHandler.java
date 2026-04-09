package com.landit.interview.voice.handler;

import com.landit.common.config.AIPromptProperties;
import com.landit.common.config.VoiceProperties;
import com.landit.common.exception.BusinessException;
import com.landit.interview.voice.dto.*;
import com.landit.interview.voice.dto.SessionState;
import com.landit.interview.voice.enums.InterviewPhaseEnum;
import com.landit.interview.voice.gateway.InterviewVoiceGateway;
import com.landit.interview.voice.service.ASRService;
import com.landit.interview.voice.service.QuestionPreGenerateService;
import com.landit.interview.voice.service.RecordingService;
import com.landit.interview.voice.service.TTSService;
import com.landit.interview.voice.service.VoiceServiceFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 面试官 Agent 处理器
 * 处理候选人回答，生成面试官回复
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewerAgentHandler {

    private final VoiceServiceFactory voiceServiceFactory;
    private final VoiceProperties voiceProperties;
    private final ChatClient.Builder chatClientBuilder;
    private final RecordingService recordingService;
    private final AIPromptProperties aiPromptProperties;
    private final QuestionPreGenerateService questionPreGenerateService;

    @Autowired
    @Lazy
    private InterviewVoiceGateway voiceGateway;

    // 会话上下文：sessionId -> ConversationContext
    private final ConcurrentHashMap<String, ConversationContext> contexts = new ConcurrentHashMap<>();

    /**
     * 处理候选人音频
     *
     * @param sessionId  会话 ID
     * @param audioData  音频数据（PCM）
     * @return 响应流（转录 + AI 回复 + 音频）
     */
    public Flux<VoiceResponse> handleCandidateAudio(String sessionId, byte[] audioData) {
        log.debug("[InterviewerAgent] Handling candidate audio, sessionId={}, size={}", sessionId, audioData.length);

        // 获取 ASR 服务
        ASRService asrService = voiceServiceFactory.getASRService();

        // 获取会话上下文（从 Gateway 加载 JD/简历）
        ConversationContext context = contexts.computeIfAbsent(sessionId, k -> {
            ConversationContext ctx = new ConversationContext();
            // 从 Gateway 获取会话状态
            SessionState state = voiceGateway.getInternalState(k);
            if (state != null) {
                ctx.setPosition(state.getPosition() != null ? state.getPosition() : "Java 开发工程师");
                ctx.setJdContent(state.getJdContent());
                ctx.setResumeContent(state.getResumeContent());
            }
            return ctx;
        });

        // 收集候选人音频数据
        context.appendCandidateAudio(audioData);

        // Step 1: ASR 识别（配置从 application.yml 读取）
        return asrService.streamRecognize(Flux.just(audioData))
                .flatMap(asrResult -> {
                    // 发送转录结果
                    Flux<VoiceResponse> transcriptFlux = Flux.just(VoiceResponse.transcript(
                            VoiceResponse.TranscriptData.builder()
                                    .text(asrResult.getText())
                                    .isFinal(asrResult.getIsFinal())
                                    .role("candidate")
                                    .confidence(asrResult.getConfidence())
                                    .build()
                    ));

                    // 如果是最终结果，根据阶段处理
                    if (asrResult.getIsFinal() && !asrResult.getText().isEmpty()) {
                        context.addCandidateMessage(asrResult.getText());

                        // 保存候选人录音片段
                        saveCandidateRecording(sessionId, context, asrResult.getText());

                        // 根据面试阶段处理
                        return transcriptFlux.concatWith(handleCandidateTranscriptByPhase(sessionId, asrResult.getText()));
                    }

                    return transcriptFlux;
                })
                .onErrorResume(e -> {
                    log.error("[InterviewerAgent] Error handling audio, sessionId={}", sessionId, e);
                    return Flux.just(VoiceResponse.error(VoiceResponse.ErrorData.builder()
                            .code("ASR_ERROR")
                            .message("语音识别失败: " + e.getMessage())
                            .build()));
                });
    }

    /**
     * 根据面试阶段处理候选人转录结果
     *
     * @param sessionId 会话 ID
     * @param text      转录文本
     * @return 响应流（面试官回复 + 音频）
     */
    private Flux<VoiceResponse> handleCandidateTranscriptByPhase(String sessionId, String text) {
        SessionState state = voiceGateway.getInternalState(sessionId);
        if (state == null) {
            log.warn("[InterviewerAgent] 会话不存在, sessionId={}", sessionId);
            return Flux.empty();
        }

        InterviewPhaseEnum phase = state.getPhase();

        // 根据阶段处理
        if (phase == InterviewPhaseEnum.WAITING_SELF_INTRODUCTION) {
            // 自我介绍阶段，结束后进入提问阶段
            log.info("[InterviewerAgent] 自我介绍完成，进入提问阶段, sessionId={}", sessionId);
            state.setPhase(InterviewPhaseEnum.ASKING_QUESTIONS);
            return generateNextQuestion(sessionId);

        } else if (phase == InterviewPhaseEnum.ASKING_QUESTIONS) {
            // 提问阶段，正常处理回答
            log.debug("[InterviewerAgent] 提问阶段，处理候选人回答, sessionId={}", sessionId);
            return generateInterviewerReply(sessionId, text);

        } else if (phase == InterviewPhaseEnum.FOLLOW_UP) {
            // 追问阶段，处理追问后的回答
            log.info("[InterviewerAgent] 追问回答完成，进入下一题, sessionId={}", sessionId);
            // 重置状态并进入下一个问题
            state.setPhase(InterviewPhaseEnum.ASKING_QUESTIONS);
            state.setFollowUpCount(0);
            state.setCurrentQuestion(state.getCurrentQuestion() + 1);

            // 检查是否还有问题
            if (state.getCurrentQuestion() < state.getTotalQuestions()) {
                return generateNextQuestion(sessionId);
            } else {
                // 面试结束
                return endInterview(sessionId);
            }

        } else if (phase == InterviewPhaseEnum.COMPLETED) {
            // 面试已结束
            log.debug("[InterviewerAgent] 面试已结束，忽略候选人输入, sessionId={}", sessionId);
            return Flux.empty();

        } else {
            log.warn("[InterviewerAgent] 未知阶段: {}, sessionId={}", phase, sessionId);
            return Flux.empty();
        }
    }

    /**
     * 处理候选人文本输入（非语音模式）
     *
     * @param sessionId 会话 ID
     * @param text      文本内容
     * @return 响应流（AI 回复 + 音频）
     */
    public Flux<VoiceResponse> handleCandidateText(String sessionId, String text) {
        log.debug("[InterviewerAgent] 处理候选人文本输入, sessionId={}, text={}", sessionId, text);

        ConversationContext context = contexts.computeIfAbsent(sessionId, k -> new ConversationContext());
        context.addCandidateMessage(text);

        // 根据阶段处理
        return handleCandidateTranscriptByPhase(sessionId, text);
    }

    /**
     * 请求自我介绍
     *
     * @param sessionId 会话 ID
     * @return 响应流（自我介绍请求文本 + 音频）
     */
    public Flux<VoiceResponse> requestSelfIntroduction(String sessionId) {
        log.debug("[InterviewerAgent] 请求自我介绍, sessionId={}", sessionId);

        SessionState state = voiceGateway.getInternalState(sessionId);
        if (state == null) {
            log.warn("[InterviewerAgent] 会话不存在, sessionId={}", sessionId);
            return Flux.error(new BusinessException("会话不存在"));
        }

        // 获取面试官风格配置
        AIPromptProperties.InterviewerStyleConfig styleConfig =
                aiPromptProperties.getVoice().getByStyle(state.getInterviewerStyle());

        // 获取自我介绍请求文本
        String prompt = styleConfig.getSelfIntroductionPromptTemplate();
        if (prompt == null || prompt.isBlank()) {
            prompt = "请先做个自我介绍吧，说说你的技术背景和项目经验。";
        }

        log.info("[InterviewerAgent] 请求自我介绍: {}, sessionId={}", prompt, sessionId);

        // 获取 TTS 服务
        TTSService ttsService = voiceServiceFactory.getTTSService();
        TTSConfig ttsConfig = buildInterviewerTTSConfig();

        // 使用分句流式合成
        return ttsService.streamSynthesizeBySentence(Flux.just(prompt), ttsConfig)
                .flatMap(ttsChunk -> {
                    // 文本响应
                    Flux<VoiceResponse> textFlux = Flux.just(VoiceResponse.transcript(
                            VoiceResponse.TranscriptData.builder()
                                    .text(ttsChunk.getText())
                                    .isFinal(ttsChunk.getIsFinal())
                                    .role("interviewer")
                                    .build()
                    ));

                    // 音频响应
                    if (ttsChunk.getAudio() != null && ttsChunk.getAudio().length > 0) {
                        String audioBase64 = Base64.getEncoder().encodeToString(ttsChunk.getAudio());
                        Flux<VoiceResponse> audioFlux = Flux.just(VoiceResponse.audio(
                                VoiceResponse.AudioData.builder()
                                        .audio(audioBase64)
                                        .format(ttsConfig.getFormat())
                                        .sampleRate(ttsConfig.getSampleRate())
                                        .build()
                        ));
                        return textFlux.concatWith(audioFlux);
                    }

                    return textFlux;
                });
    }

    /**
     * 生成下一个问题（优先使用预生成问题）
     *
     * @param sessionId 会话 ID
     * @return 响应流（问题文本 + 音频）
     */
    public Flux<VoiceResponse> generateNextQuestion(String sessionId) {
        log.debug("[InterviewerAgent] 生成下一个问题, sessionId={}", sessionId);

        SessionState state = voiceGateway.getInternalState(sessionId);
        if (state == null) {
            log.warn("[InterviewerAgent] 会话不存在, sessionId={}", sessionId);
            return Flux.error(new BusinessException("会话不存在"));
        }

        // 优先从预生成缓存获取
        Optional<PreGeneratedQuestion> cachedQuestion =
                questionPreGenerateService.getCachedQuestion(sessionId, state.getCurrentQuestion());

        if (cachedQuestion.isPresent()) {
            // 使用预生成问题（零延迟）
            String questionText = cachedQuestion.get().getText();

            // 标记已使用
            questionPreGenerateService.markQuestionUsed(sessionId, state.getCurrentQuestion());

            // 保存到上下文
            state.setLastQuestion(questionText);

            log.info("[InterviewerAgent] 使用预生成问题, sessionId={}, index={}, text={}",
                    sessionId, state.getCurrentQuestion(), questionText);

            // 获取 TTS 服务
            TTSService ttsService = voiceServiceFactory.getTTSService();
            TTSConfig ttsConfig = buildInterviewerTTSConfig();

            // 使用分句流式合成
            return ttsService.streamSynthesizeBySentence(Flux.just(questionText), ttsConfig)
                    .flatMap(ttsChunk -> {
                        // 文本响应
                        Flux<VoiceResponse> textFlux = Flux.just(VoiceResponse.transcript(
                                VoiceResponse.TranscriptData.builder()
                                        .text(ttsChunk.getText())
                                        .isFinal(ttsChunk.getIsFinal())
                                        .role("interviewer")
                                        .build()
                        ));

                        // 音频响应
                        if (ttsChunk.getAudio() != null && ttsChunk.getAudio().length > 0) {
                            String audioBase64 = Base64.getEncoder().encodeToString(ttsChunk.getAudio());
                            Flux<VoiceResponse> audioFlux = Flux.just(VoiceResponse.audio(
                                    VoiceResponse.AudioData.builder()
                                            .audio(audioBase64)
                                            .format(ttsConfig.getFormat())
                                            .sampleRate(ttsConfig.getSampleRate())
                                            .build()
                            ));
                            return textFlux.concatWith(audioFlux);
                        }

                        return textFlux;
                    });
        } else {
            // 降级到实时生成
            log.warn("[InterviewerAgent] 预生成问题未找到, 降级到实时生成, sessionId={}, index={}",
                    sessionId, state.getCurrentQuestion());
            return generateQuestionRealTime(sessionId);
        }
    }

    /**
     * 实时生成问题
     */
    private Flux<VoiceResponse> generateQuestionRealTime(String sessionId) {
        // 获取会话上下文（从 Gateway 加载 JD/简历）
        ConversationContext context = contexts.computeIfAbsent(sessionId, k -> {
            ConversationContext ctx = new ConversationContext();
            SessionState state = voiceGateway.getInternalState(k);
            if (state != null) {
                ctx.setPosition(state.getPosition() != null ? state.getPosition() : "Java 开发工程师");
                ctx.setJdContent(state.getJdContent());
                ctx.setResumeContent(state.getResumeContent());
            }
            return ctx;
        });

        SessionState state = voiceGateway.getInternalState(sessionId);

        // 根据面试官风格获取提示词配置
        String style = state.getInterviewerStyle();
        AIPromptProperties.InterviewerStyleConfig styleConfig = aiPromptProperties.getVoice().getByStyle(style);

        // 构建生成问题的提示词（包含所有上下文信息）
        String systemPrompt = styleConfig.getSystemPrompt();
        String userPrompt = styleConfig.getQuestionPromptTemplate()
                .replace("{position}", context.getPosition())
                .replace("{questionNumber}", String.valueOf(state.getCurrentQuestion() + 1))
                .replace("{totalQuestions}", String.valueOf(state.getTotalQuestions()))
                .replace("{elapsedSeconds}", String.valueOf(context.getElapsedSeconds()))
                .replace("{jdRequirements}", formatJDRequirements(context.getJdContent()))
                .replace("{resumeSummary}", formatResumeSummary(context.getResumeContent()))
                .replace("{askedQuestions}", formatAskedQuestions(context.getConversationSummary()))
                .replace("{conversationSummary}", context.getConversationSummary());

        // 调用 LLM 生成问题
        Flux<String> llmStream = callLLMStream(systemPrompt, userPrompt);

        // 获取 TTS 服务
        TTSService ttsService = voiceServiceFactory.getTTSService();
        TTSConfig ttsConfig = buildInterviewerTTSConfig();

        // 使用分句流式合成
        return ttsService.streamSynthesizeBySentence(llmStream, ttsConfig)
                .flatMap(ttsChunk -> {
                    // 文本响应
                    Flux<VoiceResponse> textFlux = Flux.just(VoiceResponse.transcript(
                            VoiceResponse.TranscriptData.builder()
                                    .text(ttsChunk.getText())
                                    .isFinal(ttsChunk.getIsFinal())
                                    .role("interviewer")
                                    .build()
                    ));

                    // 音频响应
                    if (ttsChunk.getAudio() != null && ttsChunk.getAudio().length > 0) {
                        String audioBase64 = Base64.getEncoder().encodeToString(ttsChunk.getAudio());
                        Flux<VoiceResponse> audioFlux = Flux.just(VoiceResponse.audio(
                                VoiceResponse.AudioData.builder()
                                        .audio(audioBase64)
                                        .format(ttsConfig.getFormat())
                                        .sampleRate(ttsConfig.getSampleRate())
                                        .build()
                        ));
                        return textFlux.concatWith(audioFlux);
                    }

                    return textFlux;
                });
    }

    /**
     * 生成面试官回复（含追问判断）
     */
    private Flux<VoiceResponse> generateInterviewerReply(String sessionId, String candidateAnswer) {
        log.debug("[InterviewerAgent] 生成面试官回复, sessionId={}", sessionId);

        SessionState state = voiceGateway.getInternalState(sessionId);
        if (state == null) {
            log.warn("[InterviewerAgent] 会话不存在, sessionId={}", sessionId);
            return Flux.error(new BusinessException("会话不存在"));
        }

        // 判断是否需要追问
        if (shouldAskFollowUp(candidateAnswer, state)) {
            // 生成追问
            log.info("[InterviewerAgent] 触发追问, sessionId={}, followUpCount={}", sessionId, state.getFollowUpCount());

            String followUp = questionPreGenerateService.generateFollowUpQuestion(
                    sessionId,
                    state.getLastQuestion(),
                    candidateAnswer,
                    getConversationSummary(sessionId)
            );

            // 更新追问计数
            state.setFollowUpCount(state.getFollowUpCount() + 1);
            // 设置阶段为追问中
            state.setPhase(InterviewPhaseEnum.FOLLOW_UP);

            log.info("[InterviewerAgent] 追问生成完成: {}, sessionId={}", followUp, sessionId);

            // 获取 TTS 服务
            TTSService ttsService = voiceServiceFactory.getTTSService();
            TTSConfig ttsConfig = buildInterviewerTTSConfig();

            // 使用分句流式合成
            return ttsService.streamSynthesizeBySentence(Flux.just(followUp), ttsConfig)
                    .flatMap(ttsChunk -> {
                        // 文本响应
                        Flux<VoiceResponse> textFlux = Flux.just(VoiceResponse.transcript(
                                VoiceResponse.TranscriptData.builder()
                                        .text(ttsChunk.getText())
                                        .isFinal(ttsChunk.getIsFinal())
                                        .role("interviewer")
                                        .build()
                        ));

                        // 音频响应
                        if (ttsChunk.getAudio() != null && ttsChunk.getAudio().length > 0) {
                            String audioBase64 = Base64.getEncoder().encodeToString(ttsChunk.getAudio());
                            Flux<VoiceResponse> audioFlux = Flux.just(VoiceResponse.audio(
                                    VoiceResponse.AudioData.builder()
                                            .audio(audioBase64)
                                            .format(ttsConfig.getFormat())
                                            .sampleRate(ttsConfig.getSampleRate())
                                            .build()
                            ));
                            return textFlux.concatWith(audioFlux);
                        }

                        return textFlux;
                    });
        } else {
            // 不追问，重置计数器，进入下一个问题
            state.setFollowUpCount(0);

            // 更新问题计数
            state.setCurrentQuestion(state.getCurrentQuestion() + 1);

            // 检查是否还有问题
            if (state.getCurrentQuestion() < state.getTotalQuestions()) {
                log.info("[InterviewerAgent] 进入下一个问题, sessionId={}, index={}",
                        sessionId, state.getCurrentQuestion());
                return generateNextQuestion(sessionId);
            } else {
                // 面试结束
                log.info("[InterviewerAgent] 所有问题完成，结束面试, sessionId={}", sessionId);
                return endInterview(sessionId);
            }
        }
    }

    /**
     * 判断是否需要追问
     *
     * @param answer 候选人回答
     * @param state  会话状态
     * @return 是否需要追问
     */
    private boolean shouldAskFollowUp(String answer, SessionState state) {
        // 达到追问上限
        if (state.getFollowUpCount() >= SessionState.MAX_FOLLOW_UP) {
            log.debug("[InterviewerAgent] 达到追问上限, count={}", state.getFollowUpCount());
            return false;
        }

        // 回答太短
        if (answer == null || answer.length() < 20) {
            log.debug("[InterviewerAgent] 回答太短, length={}", answer != null ? answer.length() : 0);
            return false;
        }

        // 包含结束信号
        if (answer.contains("没了") || answer.contains("就这些") || answer.contains("完了") ||
                answer.contains("没有了") || answer.contains("没有其他")) {
            log.debug("[InterviewerAgent] 包含结束信号");
            return false;
        }

        return true;
    }

    /**
     * 获取对话摘要
     *
     * @param sessionId 会话 ID
     * @return 对话摘要
     */
    private String getConversationSummary(String sessionId) {
        ConversationContext context = contexts.get(sessionId);
        if (context == null) {
            return "";
        }
        return context.getConversationSummary();
    }

    /**
     * 调用 LLM 流式生成
     */
    private Flux<String> callLLMStream(String systemPrompt, String userPrompt) {
        return chatClientBuilder.build()
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .stream()
                .content();
    }

    /**
     * 构建包含 JD 和简历的用户提示词
     */
    private String buildContextualUserPrompt(ConversationContext context, String basePrompt) {
        if (context == null) {
            return basePrompt;
        }

        StringBuilder prompt = new StringBuilder();

        // 添加 JD 内容
        if (context.getJdContent() != null && !context.getJdContent().isEmpty()) {
            prompt.append("## 职位 JD\n").append(context.getJdContent()).append("\n\n");
        }

        // 添加简历内容
        if (context.getResumeContent() != null && !context.getResumeContent().isEmpty()) {
            prompt.append("## 候选人简历\n").append(context.getResumeContent()).append("\n\n");
        }

        // 添加基础提示
        prompt.append(basePrompt);

        return prompt.toString();
    }

    /**
     * 格式化 JD 核心要求（提取关键技能和职责）
     */
    private String formatJDRequirements(String jdContent) {
        if (jdContent == null || jdContent.isBlank()) {
            return "暂无 JD 信息";
        }
        // 如果 JD 内容过长，截取前 1000 字符
        if (jdContent.length() > 1000) {
            return jdContent.substring(0, 1000) + "\n...(内容过长已截断)";
        }
        return jdContent;
    }

    /**
     * 格式化简历摘要（提取关键项目经历）
     */
    private String formatResumeSummary(String resumeContent) {
        if (resumeContent == null || resumeContent.isBlank()) {
            return "暂无简历信息";
        }
        // 如果简历内容过长，截取前 1500 字符
        if (resumeContent.length() > 1500) {
            return resumeContent.substring(0, 1500) + "\n...(内容过长已截断)";
        }
        return resumeContent;
    }

    /**
     * 格式化已提问的问题列表（从对话历史中提取）
     */
    private String formatAskedQuestions(String conversationSummary) {
        if (conversationSummary == null || conversationSummary.isBlank()) {
            return "暂无已提问的问题";
        }
        // 提取面试官的问题（以"面试官："开头的行）
        StringBuilder questions = new StringBuilder();
        String[] lines = conversationSummary.split("\n");
        int questionIndex = 1;
        for (String line : lines) {
            if (line.startsWith("面试官：")) {
                String question = line.substring("面试官：".length()).trim();
                if (!question.isEmpty()) {
                    questions.append(questionIndex++).append(". ").append(question).append("\n");
                }
            }
        }
        if (questions.length() == 0) {
            return "暂无已提问的问题";
        }
        return questions.toString();
    }

    /**
     * 构建面试官 TTS 配置
     */
    private TTSConfig buildInterviewerTTSConfig() {
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
     * 保存候选人录音片段
     */
    private void saveCandidateRecording(String sessionId, ConversationContext context, String text) {
        try {
            byte[] audioData = context.getCandidateAudioAndReset();
            if (audioData == null || audioData.length == 0) {
                log.debug("[InterviewerAgent] No candidate audio to save, sessionId={}", sessionId);
                return;
            }

            int segmentIndex = context.getNextSegmentIndex();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime = context.getSegmentStartTime();
            if (startTime == null) {
                startTime = now.minusSeconds(5);
            }

            // 估算时长：16kHz 16bit mono = 32000 bytes/sec
            int durationMs = (int) ((audioData.length / 32000.0) * 1000);

            RecordingSegment segment = RecordingSegment.builder()
                    .index(segmentIndex)
                    .role("candidate")
                    .content(text)
                    .audioData(audioData)
                    .durationMs(durationMs)
                    .startTime(startTime)
                    .endTime(now)
                    .build();

            recordingService.saveSegment(sessionId, segment);
            log.info("[InterviewerAgent] Saved candidate recording, sessionId={}, index={}, duration={}ms",
                    sessionId, segmentIndex, durationMs);
        } catch (Exception e) {
            log.error("[InterviewerAgent] Failed to save candidate recording, sessionId={}", sessionId, e);
        }
    }

    /**
     * 保存面试官录音片段
     */
    private void saveInterviewerRecording(String sessionId, ConversationContext context, String text, byte[] audioData) {
        try {
            if (audioData == null || audioData.length == 0) {
                log.debug("[InterviewerAgent] No interviewer audio to save, sessionId={}", sessionId);
                return;
            }

            int segmentIndex = context.getNextSegmentIndex();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime = context.getSegmentStartTime();
            if (startTime == null) {
                startTime = now.minusSeconds(5);
            }

            // 估算时长
            int durationMs = (int) ((audioData.length / 32000.0) * 1000);

            RecordingSegment segment = RecordingSegment.builder()
                    .index(segmentIndex)
                    .role("interviewer")
                    .content(text)
                    .audioData(audioData)
                    .durationMs(durationMs)
                    .startTime(startTime)
                    .endTime(now)
                    .build();

            recordingService.saveSegment(sessionId, segment);
            log.info("[InterviewerAgent] Saved interviewer recording, sessionId={}, index={}, duration={}ms",
                    sessionId, segmentIndex, durationMs);

            // 记录面试官消息到上下文
            context.addInterviewerMessage(text);
        } catch (Exception e) {
            log.error("[InterviewerAgent] Failed to save interviewer recording, sessionId={}", sessionId, e);
        }
    }

    /**
     * 生成追问（可选）
     * 当面试官需要对候选人回答进行深入挖掘时调用
     *
     * @param sessionId          会话 ID
     * @param lastQuestion       上一个问题
     * @param candidateAnswer    候选人回答
     * @param conversationHistory 最近对话摘要
     * @return 响应流（追问文本 + 音频）
     */
    public Flux<VoiceResponse> generateFollowUpQuestion(
            String sessionId,
            String lastQuestion,
            String candidateAnswer,
            String conversationHistory) {
        log.info("[InterviewerAgent] 生成追问, sessionId={}", sessionId);

        // 调用预生成服务生成追问
        String followUp = questionPreGenerateService.generateFollowUpQuestion(
                sessionId, lastQuestion, candidateAnswer, conversationHistory);

        // 构建响应（仅文本，不合成音频，追问应该简短）
        return Flux.just(VoiceResponse.transcript(
                VoiceResponse.TranscriptData.builder()
                        .text(followUp)
                        .isFinal(true)
                        .role("interviewer")
                        .build()
        ));
    }

    /**
     * 对话上下文
     */
    @lombok.Data
    private static class ConversationContext {
        // 面试上下文（从真实面试加载）
        private String position = "Java 开发工程师";
        private String jdContent;
        private String resumeContent;
        // 面试开始时间（用于计算已面试时长）
        private LocalDateTime interviewStartTime = LocalDateTime.now();
        // 对话历史
        private StringBuilder conversationHistory = new StringBuilder();
        private AtomicInteger segmentIndex = new AtomicInteger(0);
        private ByteArrayOutputStream candidateAudioBuffer = new ByteArrayOutputStream();
        private LocalDateTime segmentStartTime;

        public void addCandidateMessage(String message) {
            conversationHistory.append("候选人：").append(message).append("\n");
        }

        public void addInterviewerMessage(String message) {
            conversationHistory.append("面试官：").append(message).append("\n");
        }

        public String getConversationSummary() {
            if (conversationHistory.length() > 2000) {
                return conversationHistory.substring(conversationHistory.length() - 2000);
            }
            return conversationHistory.toString();
        }

        /**
         * 获取已面试时长（秒）
         */
        public long getElapsedSeconds() {
            return Duration.between(interviewStartTime, LocalDateTime.now()).getSeconds();
        }

        public int getNextSegmentIndex() {
            return segmentIndex.getAndIncrement();
        }

        public void appendCandidateAudio(byte[] audio) {
            try {
                candidateAudioBuffer.write(audio);
            } catch (Exception e) {
                log.error("Failed to append candidate audio", e);
            }
        }

        public byte[] getCandidateAudioAndReset() {
            byte[] audio = candidateAudioBuffer.toByteArray();
            candidateAudioBuffer.reset();
            return audio;
        }

        public void startSegment() {
            segmentStartTime = LocalDateTime.now();
        }

        public LocalDateTime getSegmentStartTime() {
            return segmentStartTime;
        }
    }

    /**
     * 结束面试
     *
     * @param sessionId 会话 ID
     * @return 响应流（结束语 + 音频）
     */
    public Flux<VoiceResponse> endInterview(String sessionId) {
        log.info("[InterviewerAgent] 结束面试, sessionId={}", sessionId);

        SessionState state = voiceGateway.getInternalState(sessionId);
        if (state == null) {
            log.warn("[InterviewerAgent] 会话不存在, sessionId={}", sessionId);
            return Flux.empty();
        }

        // 标记面试完成
        state.setPhase(InterviewPhaseEnum.COMPLETED);
        state.setCompleted(true);
        state.setActive(false);

        // 生成结束语
        String closingText = "好的，今天的面试就到这里。感谢你的时间，我们会尽快联系你。";
        log.info("[InterviewerAgent] 面试结束: {}, sessionId={}", closingText, sessionId);

        // 获取 TTS 服务
        TTSService ttsService = voiceServiceFactory.getTTSService();
        TTSConfig ttsConfig = buildInterviewerTTSConfig();

        // 使用分句流式合成
        return ttsService.streamSynthesizeBySentence(Flux.just(closingText), ttsConfig)
                .flatMap(ttsChunk -> {
                    // 文本响应
                    Flux<VoiceResponse> textFlux = Flux.just(VoiceResponse.transcript(
                            VoiceResponse.TranscriptData.builder()
                                    .text(ttsChunk.getText())
                                    .isFinal(ttsChunk.getIsFinal())
                                    .role("interviewer")
                                    .build()
                    ));

                    // 音频响应
                    if (ttsChunk.getAudio() != null && ttsChunk.getAudio().length > 0) {
                        String audioBase64 = Base64.getEncoder().encodeToString(ttsChunk.getAudio());
                        Flux<VoiceResponse> audioFlux = Flux.just(VoiceResponse.audio(
                                VoiceResponse.AudioData.builder()
                                        .audio(audioBase64)
                                        .format(ttsConfig.getFormat())
                                        .sampleRate(ttsConfig.getSampleRate())
                                        .build()
                        ));
                        return textFlux.concatWith(audioFlux);
                    }

                    return textFlux;
                });
    }
}
