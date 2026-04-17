package com.careerforge.interview.voice.handler;

import com.careerforge.common.config.AIPromptProperties;
import com.careerforge.common.config.VoiceProperties;
import com.careerforge.common.exception.BusinessException;
import com.careerforge.interview.voice.dto.*;
import com.careerforge.interview.voice.dto.SessionState;
import com.careerforge.interview.voice.enums.InterviewPhaseEnum;
import com.careerforge.interview.voice.enums.TranscriptRole;
import com.careerforge.interview.voice.gateway.InterviewVoiceGateway;
import com.careerforge.interview.voice.dto.ASRResult;
import com.careerforge.interview.voice.service.ASRListener;
import com.careerforge.interview.voice.service.ASRService;
import com.careerforge.interview.voice.service.QuestionPreGenerateService;
import com.careerforge.interview.voice.service.RecordingService;
import com.careerforge.interview.voice.service.TTSListener;
import com.careerforge.interview.voice.service.VoiceServiceFactory;
import com.careerforge.interview.voice.service.TTSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 面试官 Agent 处理器
 * 处理候选人回答，生成面试官回复
 * 所有方法采用回调模式，通过 voiceGateway.sendResponse() 直接推送结果
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewerAgentHandler {

    private final VoiceServiceFactory voiceServiceFactory;
    private final VoiceProperties voiceProperties;
    private final ChatClient chatClient;
    private final RecordingService recordingService;
    private final AIPromptProperties aiPromptProperties;
    private final QuestionPreGenerateService questionPreGenerateService;

    @Autowired
    @Lazy
    private InterviewVoiceGateway voiceGateway;

    private static final String SENTENCE_END_CHARS = "。！？.!;；\n";

    // 会话上下文：sessionId -> ConversationContext
    private final ConcurrentHashMap<String, ConversationContext> contexts = new ConcurrentHashMap<>();

    /**
     * 处理候选人音频
     * 音频帧通过 ASR 会话汇聚到同一条 WebSocket 连接，识别结果通过回调处理
     */
    public void handleCandidateAudio(String sessionId, byte[] audioData) {
        log.debug("[InterviewerAgent] 处理候选人音频, sessionId={}, size={}", sessionId, audioData.length);
        // 获取会话上下文（从 Gateway 加载 JD/简历）
        ConversationContext context = getOrCreateContext(sessionId);
        // 收集候选人音频数据（用于录音保存）
        context.appendCandidateAudio(audioData);
        // 获取或创建 ASR 会话（首次创建时启动并注册回调）
        ASRService asr = context.getOrCreateASRService(() -> {
            ASRService service = voiceServiceFactory.createASRService();
            service.start(new ASRListener() {
                @Override
                public void onResult(ASRResult result) {
                    handleASRResult(sessionId, result);
                }

                @Override
                public void onError(Exception e) {
                    log.error("[InterviewerAgent] ASR 会话错误, sessionId={}", sessionId, e);
                    voiceGateway.sendResponse(sessionId, VoiceResponse.error(VoiceResponse.ErrorData.builder()
                            .code("ASR_ERROR")
                            .message("语音识别失败: " + e.getMessage())
                            .build()));
                }

                @Override
                public void onComplete() {
                    log.info("[InterviewerAgent] ASR 会话结束, sessionId={}", sessionId);
                }
            });
            return service;
        });
        // 发送音频帧到 ASR 会话（复用同一条连接）
        asr.sendAudio(audioData);
    }

    /**
     * 处理 ASR 识别结果
     * 发送转录结果给客户端，isFinal 时触发面试逻辑
     */
    private void handleASRResult(String sessionId, ASRResult asrResult) {
        ConversationContext context = contexts.get(sessionId);
        if (context == null) {
            log.warn("[InterviewerAgent] 会话上下文不存在, sessionId={}", sessionId);
            return;
        }
        // 发送转录结果到客户端
        VoiceResponse transcriptResponse = VoiceResponse.transcript(
                VoiceResponse.TranscriptData.builder()
                        .text(asrResult.getText())
                        .isFinal(asrResult.getIsFinal())
                        .role(TranscriptRole.CANDIDATE.getValue())
                        .confidence(asrResult.getConfidence())
                        .build()
        );
        voiceGateway.sendResponse(sessionId, transcriptResponse);
        // 如果是最终结果且文本非空，触发面试逻辑
        if (asrResult.getIsFinal() && !asrResult.getText().isEmpty()) {
            log.info("[InterviewerAgent] 收到最终识别结果, sessionId={}, text={}", sessionId, asrResult.getText());
            context.addCandidateMessage(asrResult.getText());
            saveCandidateRecording(sessionId, context, asrResult.getText());
            // 根据面试阶段处理（结果通过回调直接推送）
            try {
                handleCandidateTranscriptByPhase(sessionId, asrResult.getText());
            } catch (Exception e) {
                log.error("[InterviewerAgent] 阶段处理错误, sessionId={}", sessionId, e);
            }
        }
    }

    /**
     * 清理会话资源（关闭 ASR/TTS 连接、移除上下文）
     */
    public void cleanupSession(String sessionId) {
        ConversationContext context = contexts.get(sessionId);
        if (context != null) {
            // 关闭 ASR 连接
            if (context.getAsrService() != null) {
                try {
                    context.getAsrService().close();
                } catch (Exception e) {
                    log.warn("[InterviewerAgent] 关闭 ASR 会话失败, sessionId={}", sessionId, e);
                }
            }
            // 关闭 TTS 连接
            TTSService tts = context.getTtsService();
            if (tts != null) {
                try {
                    tts.close();
                } catch (Exception e) {
                    log.warn("[InterviewerAgent] 关闭 TTS 会话失败, sessionId={}", sessionId, e);
                }
            }
        }
        contexts.remove(sessionId);
        log.info("[InterviewerAgent] 会话资源已清理, sessionId={}", sessionId);
    }

    /**
     * 根据面试阶段处理候选人转录结果
     */
    private void handleCandidateTranscriptByPhase(String sessionId, String text) {
        SessionState state = voiceGateway.getInternalState(sessionId);
        if (state == null) {
            log.warn("[InterviewerAgent] 会话不存在, sessionId={}", sessionId);
            return;
        }
        InterviewPhaseEnum phase = state.getPhase();
        if (phase == InterviewPhaseEnum.WAITING_SELF_INTRODUCTION) {
            log.info("[InterviewerAgent] 自我介绍完成，进入提问阶段, sessionId={}", sessionId);
            state.setPhase(InterviewPhaseEnum.ASKING_QUESTIONS);
            generateNextQuestion(sessionId);
        } else if (phase == InterviewPhaseEnum.ASKING_QUESTIONS) {
            log.debug("[InterviewerAgent] 提问阶段，处理候选人回答, sessionId={}", sessionId);
            generateInterviewerReply(sessionId, text);
        } else if (phase == InterviewPhaseEnum.FOLLOW_UP) {
            log.info("[InterviewerAgent] 追问回答完成，进入下一题, sessionId={}", sessionId);
            state.setPhase(InterviewPhaseEnum.ASKING_QUESTIONS);
            advanceToNextQuestion(sessionId, state);
        } else if (phase == InterviewPhaseEnum.COMPLETED) {
            log.debug("[InterviewerAgent] 面试已结束，忽略候选人输入, sessionId={}", sessionId);
        } else {
            log.warn("[InterviewerAgent] 未知阶段: {}, sessionId={}", phase, sessionId);
        }
    }

    /**
     * 请求自我介绍（LLM 动态生成开场白）
     */
    public void requestSelfIntroduction(String sessionId) {
        log.debug("[InterviewerAgent] 请求自我介绍, sessionId={}", sessionId);
        ConversationContext context = getOrCreateContext(sessionId);
        SessionState state = requireSession(sessionId);
        // 获取面试官风格配置
        AIPromptProperties.InterviewerStyleConfig styleConfig =
                aiPromptProperties.getVoice().getByStyle(state.getInterviewerStyle());
        String selfIntroTemplate = styleConfig.getSelfIntroPromptTemplate();
        if (selfIntroTemplate == null || selfIntroTemplate.isBlank()) {
            // 降级：使用静态模板
            String fallback = styleConfig.getSelfIntroductionPromptTemplate();
            if (fallback == null || fallback.isBlank()) {
                fallback = "请先做个自我介绍吧，说说你的技术背景和项目经验。";
            }
            log.info("[InterviewerAgent] 自我介绍降级使用静态模板: {}, sessionId={}", fallback, sessionId);
            recordInterviewerMessage(sessionId, fallback);
            synthesizeAndSend(sessionId, fallback, false);
            return;
        }
        // 构建提示词（复用已有的上下文格式化方法）
        String systemPrompt = styleConfig.getSystemPrompt();
        String userPrompt = selfIntroTemplate
                .replace("{position}", context.getPosition())
                .replace("{jdRequirements}", formatJDRequirements(context.getJdContent()))
                .replace("{resumeSummary}", formatResumeSummary(context.getResumeContent()));
        log.info("[InterviewerAgent] 使用 LLM 动态生成自我介绍开场白, sessionId={}", sessionId);
        // LLM 流式生成（synthesizeStreamAndSend 内部 onComplete 会记录对话历史）
        Flux<String> llmStream = callLLMStream(systemPrompt, userPrompt);
        synthesizeStreamAndSend(sessionId, llmStream);
    }

    /**
     * 生成下一个问题（预生成问题作为参考，LLM 结合上下文微调）
     */
    public void generateNextQuestion(String sessionId) {
        log.debug("[InterviewerAgent] 生成下一个问题, sessionId={}", sessionId);
        ConversationContext context = getOrCreateContext(sessionId);
        SessionState state = requireSession(sessionId);
        // 尝试获取预设问题作为参考
        Optional<PreGeneratedQuestion> cachedQuestion =
                questionPreGenerateService.getCachedQuestion(sessionId, state.getCurrentQuestion());
        String preGeneratedText;
        if (cachedQuestion.isPresent()) {
            questionPreGenerateService.markQuestionUsed(sessionId, state.getCurrentQuestion());
            preGeneratedText = cachedQuestion.get().getText();
            log.info("[InterviewerAgent] 使用预设问题作为参考, sessionId={}, index={}, text={}",
                    sessionId, state.getCurrentQuestion(), preGeneratedText);
        } else {
            preGeneratedText = "无";
            log.warn("[InterviewerAgent] 预设问题未找到, 从零生成, sessionId={}, index={}",
                    sessionId, state.getCurrentQuestion());
        }
        // 根据面试官风格获取提示词配置
        String style = state.getInterviewerStyle();
        AIPromptProperties.InterviewerStyleConfig styleConfig = aiPromptProperties.getVoice().getByStyle(style);
        // 构建提示词（注入预设问题 + 面试上下文）
        String systemPrompt = styleConfig.getSystemPrompt();
        String userPrompt = styleConfig.getQuestionPromptTemplate()
                .replace("{position}", context.getPosition())
                .replace("{questionNumber}", String.valueOf(state.getCurrentQuestion() + 1))
                .replace("{totalQuestions}", String.valueOf(state.getTotalQuestions()))
                .replace("{elapsedSeconds}", String.valueOf(context.getElapsedSeconds()))
                .replace("{jdRequirements}", formatJDRequirements(context.getJdContent()))
                .replace("{resumeSummary}", formatResumeSummary(context.getResumeContent()))
                .replace("{askedQuestions}", formatAskedQuestions(context.getConversationSummary()))
                .replace("{conversationSummary}", context.getConversationSummary())
                .replace("{preGeneratedQuestion}", preGeneratedText);
        // 调用 LLM 流式生成
        Flux<String> llmStream = callLLMStream(systemPrompt, userPrompt);
        synthesizeStreamAndSend(sessionId, llmStream);
    }

    /**
     * 生成面试官回复（含追问判断）
     */
    private void generateInterviewerReply(String sessionId, String candidateAnswer) {
        log.debug("[InterviewerAgent] 生成面试官回复, sessionId={}", sessionId);
        SessionState state = requireSession(sessionId);
        // 判断是否需要追问
        if (shouldAskFollowUp(candidateAnswer, state)) {
            log.info("[InterviewerAgent] 触发追问, sessionId={}, followUpCount={}", sessionId, state.getFollowUpCount());
            String followUp = questionPreGenerateService.generateFollowUpQuestion(
                    sessionId,
                    state.getLastQuestion(),
                    candidateAnswer,
                    getConversationSummary(sessionId)
            );
            state.setFollowUpCount(state.getFollowUpCount() + 1);
            state.setPhase(InterviewPhaseEnum.FOLLOW_UP);
            log.info("[InterviewerAgent] 追问生成完成: {}, sessionId={}", followUp, sessionId);
            recordInterviewerMessage(sessionId, followUp);
            synthesizeAndSend(sessionId, followUp, false);
        } else {
            advanceToNextQuestion(sessionId, state);
        }
    }

    /**
     * 判断是否需要追问
     */
    private boolean shouldAskFollowUp(String answer, SessionState state) {
        if (state.getFollowUpCount() >= SessionState.MAX_FOLLOW_UP) {
            return false;
        }
        if (answer == null || answer.length() < 20) {
            return false;
        }
        return !answer.contains("没了") && !answer.contains("就这些") && !answer.contains("完了") &&
                !answer.contains("没有了") && !answer.contains("没有其他");
    }

    /**
     * 获取完整对话历史（面试结束时使用）
     */
    public String getFullConversationHistory(String sessionId) {
        ConversationContext context = contexts.get(sessionId);
        if (context == null) {
            return "";
        }
        return context.getConversationHistory().toString();
    }

    /**
     * 获取对话摘要
     */
    private String getConversationSummary(String sessionId) {
        ConversationContext context = contexts.get(sessionId);
        if (context == null) {
            return "";
        }
        return context.getConversationSummary();
    }

    /**
     * 调用 LLM 流式生成（Flux 来自 Spring AI API，无法避免）
     */
    private Flux<String> callLLMStream(String systemPrompt, String userPrompt) {
        return chatClient
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .stream()
                .content();
    }

    /**
     * 静态文本合成并推送
     * 获取 session-scoped TTS，合成文本，音频通过 gateway 直接推送
     *
     * @param sessionId 会话 ID
     * @param text      要合成的文本
     * @param isFinal   是否最终标记（面试结束时使用）
     */
    private void synthesizeAndSend(String sessionId, String text, boolean isFinal) {
        ConversationContext context = getOrCreateContext(sessionId);
        TTSConfig ttsConfig = buildInterviewerTTSConfig();
        TTSService tts = context.getOrCreateTTSService(() -> {
            TTSService service = voiceServiceFactory.createTTSService(ttsConfig);
            service.connect();
            return service;
        });
        // 异步执行 TTS 合成，避免阻塞调用线程
        CompletableFuture.runAsync(() -> {
            // 先发送文本转录
            voiceGateway.sendResponse(sessionId, VoiceResponse.transcript(
                    VoiceResponse.TranscriptData.builder()
                            .text(text)
                            .isFinal(false)
                            .role(TranscriptRole.INTERVIEWER.getValue())
                            .build()
            ));
            // 合成音频并推送
            tts.synthesize(text, new TTSListener() {
                @Override
                public void onAudio(byte[] audioData) {
                    String audioBase64 = Base64.getEncoder().encodeToString(audioData);
                    voiceGateway.sendResponse(sessionId, VoiceResponse.audio(
                            VoiceResponse.AudioData.builder()
                                    .audio(audioBase64)
                                    .format(ttsConfig.getFormat())
                                    .sampleRate(ttsConfig.getSampleRate())
                                    .build()
                    ));
                }

                @Override
                public void onError(Exception e) {
                    log.error("[InterviewerAgent] TTS合成错误, sessionId={}, text={}", sessionId, text, e);
                }

                @Override
                public void onComplete() {
                    // 发送最终标记
                    voiceGateway.sendResponse(sessionId, VoiceResponse.transcript(
                            VoiceResponse.TranscriptData.builder()
                                    .text("")
                                    .isFinal(isFinal)
                                    .role(TranscriptRole.INTERVIEWER.getValue())
                                    .build()
                    ));
                }
            });
        });
    }

    /**
     * LLM 文本流分句合成并推送
     * 订阅 LLM Flux，按句子分割，逐句通过 session-scoped TTS 合成音频
     *
     * @param sessionId  会话 ID
     * @param textStream LLM 文本流
     */
    private void synthesizeStreamAndSend(String sessionId, Flux<String> textStream) {
        ConversationContext context = getOrCreateContext(sessionId);
        TTSConfig ttsConfig = buildInterviewerTTSConfig();
        TTSService tts = context.getOrCreateTTSService(() -> {
            TTSService service = voiceServiceFactory.createTTSService(ttsConfig);
            service.connect();
            return service;
        });
        StringBuilder sentenceBuffer = new StringBuilder();
        StringBuilder fullText = new StringBuilder();
        textStream.subscribe(
                delta -> {
                    sentenceBuffer.append(delta);
                    // 遇到句子结束符时立即合成当前句子
                    if (isSentenceEnd(delta)) {
                        String sentence = sentenceBuffer.toString();
                        sentenceBuffer.setLength(0);
                        fullText.append(sentence);
                        // 发送文本转录
                        voiceGateway.sendResponse(sessionId, VoiceResponse.transcript(
                                VoiceResponse.TranscriptData.builder()
                                        .text(sentence)
                                        .isFinal(false)
                                        .role(TranscriptRole.INTERVIEWER.getValue())
                                        .build()
                        ));
                        // 同步合成音频（synthesisLock 保证串行）
                        synthesizeSentenceAudio(sessionId, tts, sentence, ttsConfig, false);
                    }
                },
                error -> log.error("[InterviewerAgent] LLM流错误, sessionId={}", sessionId, error),
                () -> {
                    // 处理剩余文本
                    if (sentenceBuffer.length() > 0) {
                        String lastSentence = sentenceBuffer.toString();
                        fullText.append(lastSentence);
                        voiceGateway.sendResponse(sessionId, VoiceResponse.transcript(
                                VoiceResponse.TranscriptData.builder()
                                        .text(lastSentence)
                                        .isFinal(false)
                                        .role(TranscriptRole.INTERVIEWER.getValue())
                                        .build()
                        ));
                        synthesizeSentenceAudio(sessionId, tts, lastSentence, ttsConfig, true);
                    } else {
                        // LLM 回复以句号结尾时 buffer 为空，仍需发送 final 信号
                        voiceGateway.sendResponse(sessionId, VoiceResponse.transcript(
                                VoiceResponse.TranscriptData.builder()
                                        .text("")
                                        .isFinal(true)
                                        .role(TranscriptRole.INTERVIEWER.getValue())
                                        .build()
                        ));
                    }
                    // 记录完整问题文本到对话历史
                    if (fullText.length() > 0) {
                        context.addInterviewerMessage(fullText.toString());
                    }
                }
        );
    }

    /**
     * 合成单个句子的音频并推送到客户端
     * 使用 session-scoped TTS 连接，synthesisLock 保证串行
     */
    private void synthesizeSentenceAudio(String sessionId, TTSService tts,
                                          String sentence, TTSConfig ttsConfig, boolean isFinal) {
        // 用 CountDownLatch 等待合成完成，保证句子按顺序推送
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        tts.synthesize(sentence, new TTSListener() {
            @Override
            public void onAudio(byte[] audioData) {
                String audioBase64 = Base64.getEncoder().encodeToString(audioData);
                voiceGateway.sendResponse(sessionId, VoiceResponse.audio(
                        VoiceResponse.AudioData.builder()
                                .audio(audioBase64)
                                .format(ttsConfig.getFormat())
                                .sampleRate(ttsConfig.getSampleRate())
                                .build()
                ));
            }

            @Override
            public void onError(Exception e) {
                log.error("[InterviewerAgent] TTS合成错误, sentence={}", sentence, e);
                latch.countDown();
            }

            @Override
            public void onComplete() {
                if (isFinal) {
                    voiceGateway.sendResponse(sessionId, VoiceResponse.transcript(
                            VoiceResponse.TranscriptData.builder()
                                    .text("")
                                    .isFinal(true)
                                    .role(TranscriptRole.INTERVIEWER.getValue())
                                    .build()
                    ));
                }
                latch.countDown();
            }
        });
        // 等待合成完成，超时30秒
        try {
            if (!latch.await(30, java.util.concurrent.TimeUnit.SECONDS)) {
                log.warn("[InterviewerAgent] TTS合成超时, sentence={}", sentence);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[InterviewerAgent] TTS合成被中断, sentence={}", sentence);
        }
    }

    /**
     * 获取或创建会话上下文
     */
    private ConversationContext getOrCreateContext(String sessionId) {
        return contexts.computeIfAbsent(sessionId, k -> {
            ConversationContext ctx = new ConversationContext();
            SessionState state = voiceGateway.getInternalState(k);
            if (state != null) {
                ctx.setPosition(state.getPosition() != null ? state.getPosition() : "Java 开发工程师");
                ctx.setJdContent(state.getJdContent());
                ctx.setResumeContent(state.getResumeContent());
            }
            return ctx;
        });
    }

    /**
     * 获取必须存在的会话状态
     */
    private SessionState requireSession(String sessionId) {
        SessionState state = voiceGateway.getInternalState(sessionId);
        if (state == null) {
            log.warn("[InterviewerAgent] 会话不存在, sessionId={}", sessionId);
            throw new BusinessException("会话不存在");
        }
        return state;
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
     * 记录面试官消息到对话历史
     */
    private void recordInterviewerMessage(String sessionId, String message) {
        ConversationContext ctx = contexts.get(sessionId);
        if (ctx != null) {
            ctx.addInterviewerMessage(message);
        }
    }

    /**
     * 保存候选人录音片段
     */
    private void saveCandidateRecording(String sessionId, ConversationContext context, String text) {
        try {
            byte[] audioData = context.getCandidateAudioAndReset();
            if (audioData == null || audioData.length == 0) {
                log.debug("[InterviewerAgent] 无候选人录音需要保存, sessionId={}", sessionId);
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
                    .role(TranscriptRole.CANDIDATE.getValue())
                    .content(text)
                    .audioData(audioData)
                    .durationMs(durationMs)
                    .startTime(startTime)
                    .endTime(now)
                    .build();
            recordingService.saveSegment(sessionId, segment);
            log.info("[InterviewerAgent] 保存候选人录音, sessionId={}, index={}, duration={}ms",
                    sessionId, segmentIndex, durationMs);
        } catch (Exception e) {
            log.error("[InterviewerAgent] 保存候选人录音失败, sessionId={}", sessionId, e);
        }
    }

    /**
     * 推进到下一个问题或结束面试
     */
    private void advanceToNextQuestion(String sessionId, SessionState state) {
        state.setFollowUpCount(0);
        state.setCurrentQuestion(state.getCurrentQuestion() + 1);
        if (state.getCurrentQuestion() < state.getTotalQuestions()) {
            log.info("[InterviewerAgent] 进入下一个问题, sessionId={}, index={}", sessionId, state.getCurrentQuestion());
            generateNextQuestion(sessionId);
        } else {
            log.info("[InterviewerAgent] 所有问题完成，结束面试, sessionId={}", sessionId);
            endInterview(sessionId);
        }
    }

    /**
     * 结束面试
     */
    public void endInterview(String sessionId) {
        log.info("[InterviewerAgent] 结束面试, sessionId={}", sessionId);
        SessionState state = voiceGateway.getInternalState(sessionId);
        if (state == null) {
            log.warn("[InterviewerAgent] 会话不存在, sessionId={}", sessionId);
            return;
        }
        // 标记面试完成
        state.setPhase(InterviewPhaseEnum.COMPLETED);
        state.complete();
        // 生成结束语
        String closingText = "好的，今天的面试就到这里。感谢你的时间，我们会尽快联系你。";
        log.info("[InterviewerAgent] 面试结束: {}, sessionId={}", closingText, sessionId);
        recordInterviewerMessage(sessionId, closingText);
        synthesizeAndSend(sessionId, closingText, true);
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
     * 格式化已提问的问题列表
     */
    private String formatAskedQuestions(String conversationSummary) {
        if (conversationSummary == null || conversationSummary.isBlank()) {
            return "暂无已提问的问题";
        }
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
}
