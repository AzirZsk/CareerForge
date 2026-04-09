package com.landit.interview.voice.handler.impl;

import com.landit.common.config.AIPromptProperties;
import com.landit.common.config.VoiceProperties;
import com.landit.interview.voice.dto.*;
import com.landit.interview.voice.gateway.impl.InterviewVoiceGatewayImpl;
import com.landit.interview.voice.handler.InterviewerAgentHandler;
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
 * 面试官 Agent 处理器实现
 * 处理候选人回答，生成面试官回复
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewerAgentHandlerImpl implements InterviewerAgentHandler {

    private final VoiceServiceFactory voiceServiceFactory;
    private final VoiceProperties voiceProperties;
    private final ChatClient.Builder chatClientBuilder;
    private final RecordingService recordingService;
    private final AIPromptProperties aiPromptProperties;
    private final QuestionPreGenerateService questionPreGenerateService;

    @Autowired
    @Lazy
    private InterviewVoiceGatewayImpl voiceGateway;

    // 会话上下文：sessionId -> ConversationContext
    private final ConcurrentHashMap<String, ConversationContext> contexts = new ConcurrentHashMap<>();

    @Override
    public Flux<VoiceResponse> handleCandidateAudio(String sessionId, byte[] audioData) {
        log.debug("[InterviewerAgent] Handling candidate audio, sessionId={}, size={}", sessionId, audioData.length);

        // 获取 ASR 服务
        ASRService asrService = voiceServiceFactory.getASRService();

        // 获取会话上下文（从 Gateway 加载 JD/简历）
        ConversationContext context = contexts.computeIfAbsent(sessionId, k -> {
            ConversationContext ctx = new ConversationContext();
            // 从 Gateway 获取会话状态
            InterviewVoiceGatewayImpl.SessionState state = voiceGateway.getInternalState(k);
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

                    // 如果是最终结果，保存候选人录音并生成面试官回复
                    if (asrResult.getIsFinal() && !asrResult.getText().isEmpty()) {
                        context.addCandidateMessage(asrResult.getText());

                        // 保存候选人录音片段
                        saveCandidateRecording(sessionId, context, asrResult.getText());

                        return transcriptFlux.concatWith(generateInterviewerReply(sessionId, asrResult.getText()));
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

    @Override
    public Flux<VoiceResponse> handleCandidateText(String sessionId, String text) {
        log.debug("[InterviewerAgent] Handling candidate text, sessionId={}", sessionId);

        ConversationContext context = contexts.computeIfAbsent(sessionId, k -> new ConversationContext());
        context.addCandidateMessage(text);

        return generateInterviewerReply(sessionId, text);
    }

    @Override
    public Flux<VoiceResponse> generateNextQuestion(String sessionId) {
        log.debug("[InterviewerAgent] Generating next question, sessionId={}", sessionId);

        // 获取当前问题索引
        InterviewVoiceGatewayImpl.SessionState state = voiceGateway.getInternalState(sessionId);
        int questionIndex = state != null ? state.getCurrentQuestion() : 0;

        // 优先检查缓存
        Optional<PreGeneratedQuestion> cachedQuestion = questionPreGenerateService
                .getCachedQuestion(sessionId, questionIndex);

        if (cachedQuestion.isPresent()) {
            PreGeneratedQuestion question = cachedQuestion.get();
            questionPreGenerateService.markQuestionUsed(sessionId, questionIndex);
            log.info("[InterviewerAgent] 使用缓存问题（零延迟）, sessionId={}, index={}", sessionId, questionIndex);
            return buildResponseFromCache(question);
        }

        // 无缓存，实时生成
        log.debug("[InterviewerAgent] 缓存未命中，实时生成, sessionId={}, index={}", sessionId, questionIndex);
        return generateQuestionRealTime(sessionId);
    }

    /**
     * 从缓存构建响应（零延迟）
     * 预生成只包含文本，音频由 Gateway 根据 voiceMode 决定是否实时合成
     */
    private Flux<VoiceResponse> buildResponseFromCache(PreGeneratedQuestion question) {
        return Flux.just(VoiceResponse.transcript(
                VoiceResponse.TranscriptData.builder()
                        .text(question.getText())
                        .isFinal(true)
                        .role("interviewer")
                        .build()
        ));
    }

    /**
     * 实时生成问题
     */
    private Flux<VoiceResponse> generateQuestionRealTime(String sessionId) {
        // 获取会话上下文（从 Gateway 加载 JD/简历）
        ConversationContext context = contexts.computeIfAbsent(sessionId, k -> {
            ConversationContext ctx = new ConversationContext();
            InterviewVoiceGatewayImpl.SessionState state = voiceGateway.getInternalState(k);
            if (state != null) {
                ctx.setPosition(state.getPosition() != null ? state.getPosition() : "Java 开发工程师");
                ctx.setJdContent(state.getJdContent());
                ctx.setResumeContent(state.getResumeContent());
            }
            return ctx;
        });

        InterviewVoiceGatewayImpl.SessionState state = voiceGateway.getInternalState(sessionId);

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
     * 生成面试官回复
     */
    private Flux<VoiceResponse> generateInterviewerReply(String sessionId, String candidateAnswer) {
        log.debug("[InterviewerAgent] Generating interviewer reply, sessionId={}", sessionId);

        ConversationContext context = contexts.get(sessionId);
        if (context == null) {
            return Flux.empty();
        }

        // 开始新的录音片段
        context.startSegment();

        // 根据面试官风格获取提示词配置
        InterviewVoiceGatewayImpl.SessionState state = voiceGateway.getInternalState(sessionId);
        String style = state != null ? state.getInterviewerStyle() : "professional";
        AIPromptProperties.InterviewerStyleConfig styleConfig = aiPromptProperties.getVoice().getByStyle(style);

        // 构建提示词
        String systemPrompt = styleConfig.getSystemPrompt();
        String userPrompt = styleConfig.getReplyPromptTemplate()
                .replace("{candidateAnswer}", candidateAnswer)
                .replace("{questionNumber}", String.valueOf(state != null ? state.getCurrentQuestion() : 1))
                .replace("{totalQuestions}", String.valueOf(state != null ? state.getTotalQuestions() : 5))
                .replace("{elapsedSeconds}", String.valueOf(context.getElapsedSeconds()))
                .replace("{jdRequirements}", formatJDRequirements(context.getJdContent()));

        // 调用 LLM 生成回复
        Flux<String> llmStream = callLLMStream(systemPrompt, userPrompt);

        // 获取 TTS 服务
        TTSService ttsService = voiceServiceFactory.getTTSService();
        TTSConfig ttsConfig = buildInterviewerTTSConfig();

        // 用于收集完整的面试官回复文本和音频
        StringBuilder fullText = new StringBuilder();
        ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();

        // 使用分句流式合成
        return ttsService.streamSynthesizeBySentence(llmStream, ttsConfig)
                .flatMap(ttsChunk -> {
                    // 收集文本
                    fullText.append(ttsChunk.getText());

                    // 收集音频
                    if (ttsChunk.getAudio() != null && ttsChunk.getAudio().length > 0) {
                        try {
                            audioBuffer.write(ttsChunk.getAudio());
                        } catch (Exception e) {
                            log.error("[InterviewerAgent] Failed to buffer audio", e);
                        }
                    }

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
                })
                .doOnComplete(() -> {
                    // 更新问题计数（使用外部已定义的 state 变量）
                    if (state != null) {
                        state.setCurrentQuestion(state.getCurrentQuestion() + 1);
                    }

                    // 保存面试官录音片段
                    saveInterviewerRecording(sessionId, context, fullText.toString(), audioBuffer.toByteArray());
                });
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

    @Override
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
}
