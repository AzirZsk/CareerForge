package com.careerforge.interview.voice.handler;

import com.careerforge.common.config.prompt.VoiceInterviewPromptProperties;
import com.careerforge.common.config.VoiceProperties;
import com.careerforge.common.exception.BusinessException;
import com.careerforge.interview.voice.dto.*;
import com.careerforge.interview.voice.dto.SessionState;
import com.careerforge.interview.voice.enums.InterviewPhaseEnum;
import com.careerforge.interview.voice.enums.TranscriptRole;
import com.careerforge.interview.voice.enums.VoiceRole;
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
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
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
    private final VoiceInterviewPromptProperties voicePromptProperties;
    private final QuestionPreGenerateService questionPreGenerateService;

    @Autowired
    @Lazy
    private InterviewVoiceGateway voiceGateway;

    // 会话上下文：sessionId -> ConversationContext
    private final ConcurrentHashMap<String, ConversationContext> contexts = new ConcurrentHashMap<>();

    /**
     * 处理候选人音频
     * 音频帧通过 ASR 会话汇聚到同一条 WebSocket 连接，识别结果通过回调处理
     */
    public void handleCandidateAudio(String sessionId, byte[] audioData) {
        ConversationContext context = getOrCreateContext(sessionId);
        context.appendCandidateAudio(audioData);
        // ASR 已在 initSession 中创建，连接断开时自动重建
        ASRService asr = context.getOrCreateASRService(() -> createASRServiceForSession(sessionId));
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
     * 初始化会话资源（提前创建 ASR/TTS 连接）
     * 在面试开始时调用，避免首次使用时的建连延迟
     *
     * @param sessionId 会话ID
     */
    public void initSession(String sessionId) {
        log.info("[InterviewerAgent] 初始化会话资源, sessionId={}, voiceMode={}", sessionId,
                isFullVoiceMode(sessionId) ? "full_voice" : "half_voice");
        ConversationContext context = getOrCreateContext(sessionId);
        // half_voice 模式不需要 TTS，面试官只回复文字
        if (isFullVoiceMode(sessionId)) {
            TTSService ttsService = createTTSServiceForSession(sessionId);
            context.setTtsService(ttsService);
        }
        // 提前创建 ASR 连接（两种模式都需要，用户语音输入）
        ASRService asrService = createASRServiceForSession(sessionId);
        context.setAsrService(asrService);
        log.info("[InterviewerAgent] 会话资源初始化完成, sessionId={}", sessionId);
    }

    /**
     * 创建并连接 TTS 服务
     */
    private TTSService createTTSServiceForSession(String sessionId) {
        VoiceProperties.AliyunConfig.TTSConfig ttsProps = voiceProperties.getAliyun().getTts();
        TTSService service = voiceServiceFactory.createTTSService(VoiceRole.INTERVIEWER);
        service.connect(new TTSListener() {
            @Override
            public void onAudio(byte[] audioData) {
                String audioBase64 = Base64.getEncoder().encodeToString(audioData);
                voiceGateway.sendResponse(sessionId, VoiceResponse.audio(
                        VoiceResponse.AudioData.builder()
                                .audio(audioBase64)
                                .format(ttsProps.getFormat())
                                .sampleRate(ttsProps.getSampleRate())
                                .build()
                ));
            }

            @Override
            public void onError(Exception e) {
                log.error("[InterviewerAgent] TTS合成错误, sessionId={}", sessionId, e);
            }

            @Override
            public void onComplete() {
                log.info("[InterviewerAgent] TTS连接关闭, sessionId={}", sessionId);
            }
        });
        return service;
    }

    /**
     * 创建并启动 ASR 服务
     */
    private ASRService createASRServiceForSession(String sessionId) {
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
            log.info("[InterviewerAgent] 自我介绍完成，进入回应阶段, sessionId={}", sessionId);
            state.setPhase(InterviewPhaseEnum.SELF_INTRO_RESPONSE);
            generateSelfIntroResponse(sessionId, text);
        } else if (phase == InterviewPhaseEnum.SELF_INTRO_RESPONSE) {
            // AI正在回应自我介绍，忽略候选人输入
            log.debug("[InterviewerAgent] 自我介绍回应中，忽略候选人输入, sessionId={}", sessionId);
        } else if (phase == InterviewPhaseEnum.ASKING_QUESTIONS) {
            log.debug("[InterviewerAgent] 提问阶段，处理候选人回答, sessionId={}", sessionId);
            generateInterviewerReply(sessionId, text);
        } else if (phase == InterviewPhaseEnum.FOLLOW_UP) {
            log.info("[InterviewerAgent] 追问回答完成，进入下一题, sessionId={}", sessionId);
            state.setPhase(InterviewPhaseEnum.ASKING_QUESTIONS);
            // 发送过渡回复后推进到下一题
            String transition = buildFollowUpTransition(sessionId);
            recordInterviewerMessage(sessionId, transition);
            synthesizeAndSend(sessionId, transition, false);
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
        VoiceInterviewPromptProperties.InterviewerStyleConfig styleConfig =
                voicePromptProperties.getByStyle(state.getInterviewerStyle());
        String systemPrompt = styleConfig.getSystemPrompt();
        String userPrompt = styleConfig.getSelfIntroPromptTemplate()
                .replace("{companyName}", context.getCompanyName() != null ? context.getCompanyName() : "")
                .replace("{position}", context.getPosition())
                .replace("{jdRequirements}", formatJDRequirements(context.getJdContent()))
                .replace("{resumeSummary}", formatResumeSummary(context.getResumeContent()));
        log.info("[InterviewerAgent] 使用 LLM 动态生成自我介绍开场白, sessionId={}", sessionId);
        Flux<String> llmStream = callLLMStream(systemPrompt, userPrompt);
        synthesizeStreamAndSend(sessionId, llmStream);
    }

    /**
     * 生成自我介绍后的个性化回应（流式），回应完成后自然过渡到第一个问题
     */
    private void generateSelfIntroResponse(String sessionId, String selfIntroText) {
        log.debug("[InterviewerAgent] 生成自我介绍回应, sessionId={}", sessionId);
        ConversationContext context = getOrCreateContext(sessionId);
        SessionState state = requireSession(sessionId);
        VoiceInterviewPromptProperties.InterviewerStyleConfig styleConfig =
                voicePromptProperties.getByStyle(state.getInterviewerStyle());
        String systemPrompt = styleConfig.getSystemPrompt();
        String userPrompt = styleConfig.getSelfIntroResponsePromptTemplate()
                .replace("{position}", context.getPosition())
                .replace("{jdRequirements}", formatJDRequirements(context.getJdContent()))
                .replace("{resumeSummary}", formatResumeSummary(context.getResumeContent()))
                .replace("{candidateSelfIntro}", selfIntroText);
        try {
            Flux<String> llmStream = callLLMStream(systemPrompt, userPrompt);
            synthesizeStreamAndSend(sessionId, llmStream, fullText -> {
                log.info("[InterviewerAgent] 自我介绍回应完成，进入提问阶段, sessionId={}", sessionId);
                state.setPhase(InterviewPhaseEnum.ASKING_QUESTIONS);
                generateNextQuestion(sessionId);
            });
        } catch (Exception e) {
            log.error("[InterviewerAgent] 自我介绍回应生成失败，直接进入提问, sessionId={}", sessionId, e);
            state.setPhase(InterviewPhaseEnum.ASKING_QUESTIONS);
            generateNextQuestion(sessionId);
        }
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
        VoiceInterviewPromptProperties.InterviewerStyleConfig styleConfig = voicePromptProperties.getByStyle(style);
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
        // 调用 LLM 流式生成，完成后记录当前问题到 lastQuestion
        Flux<String> llmStream = callLLMStream(systemPrompt, userPrompt);
        synthesizeStreamAndSend(sessionId, llmStream, text -> {
            if (text != null && !text.isBlank()) {
                state.setLastQuestion(text);
                log.debug("[InterviewerAgent] 更新lastQuestion, sessionId={}, length={}", sessionId, text.length());
            }
        });
    }

    /**
     * 生成面试官回复（LLM 语义追问判断 + 反应生成）
     */
    private void generateInterviewerReply(String sessionId, String candidateAnswer) {
        log.debug("[InterviewerAgent] 生成面试官回复, sessionId={}", sessionId);
        SessionState state = requireSession(sessionId);
        ConversationContext context = getOrCreateContext(sessionId);
        // LLM 语义判断 + 反应生成 + 追问生成（单次调用）
        FollowUpJudgeResponse judgeResponse = questionPreGenerateService.judgeAndGenerateFollowUp(
                sessionId,
                state.getLastQuestion(),
                candidateAnswer,
                getConversationSummary(sessionId),
                formatJDRequirements(context.getJdContent()),
                state.getFollowUpCount()
        );

        if (judgeResponse.isNeedFollowUp() && judgeResponse.getFollowUpQuestion() != null) {
            // 追问路径：拼接反应 + 追问，一次性发送
            String fullReply = buildReplyWithReaction(judgeResponse.getReplyReaction(), judgeResponse.getFollowUpQuestion());
            log.info("[InterviewerAgent] 触发追问（LLM判断）, sessionId={}, followUpCount={}, reason={}",
                    sessionId, state.getFollowUpCount(), judgeResponse.getReason());
            state.setFollowUpCount(state.getFollowUpCount() + 1);
            state.setPhase(InterviewPhaseEnum.FOLLOW_UP);
            state.setLastQuestion(judgeResponse.getFollowUpQuestion());
            recordInterviewerMessage(sessionId, fullReply);
            synthesizeAndSend(sessionId, fullReply, false);
        } else {
            // 不追问路径：先发送反应，再推进到下一题
            String reaction = judgeResponse.getReplyReaction();
            log.info("[InterviewerAgent] 不追问（LLM判断）, sessionId={}, reason={}",
                    sessionId, judgeResponse.getReason());
            if (reaction != null && !reaction.isBlank()) {
                recordInterviewerMessage(sessionId, reaction);
                synthesizeAndSend(sessionId, reaction, false);
            }
            advanceToNextQuestion(sessionId, state);
        }
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
                .options(OpenAiChatOptions.builder()
                        .extraBody(Map.of("enable_thinking", false))
                        .build())
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
        // 发送文本转录
        voiceGateway.sendResponse(sessionId, VoiceResponse.transcript(
                VoiceResponse.TranscriptData.builder()
                        .text(text)
                        .isFinal(false)
                        .role(TranscriptRole.INTERVIEWER.getValue())
                        .build()
        ));
        // full_voice 模式才合成语音
        if (isFullVoiceMode(sessionId)) {
            TTSService tts = context.getOrCreateTTSService(() -> createTTSServiceForSession(sessionId));
            tts.synthesize(text, true);
        }
        // 发送最终标记
        voiceGateway.sendResponse(sessionId, VoiceResponse.transcript(
                VoiceResponse.TranscriptData.builder()
                        .text("")
                        .isFinal(isFinal)
                        .role(TranscriptRole.INTERVIEWER.getValue())
                        .build()
        ));
    }

    /**
     * LLM 文本流流式合成并推送
     * 利用 TTS server_commit 模式，将 LLM delta 直接推送到 TTS 服务端
     * 服务端自动检测句子边界并开始合成，消除应用层分句延迟
     *
     * @param sessionId  会话 ID
     * @param textStream LLM 文本流
     */
    private void synthesizeStreamAndSend(String sessionId, Flux<String> textStream) {
        synthesizeStreamAndSend(sessionId, textStream, null);
    }

    private void synthesizeStreamAndSend(String sessionId, Flux<String> textStream, java.util.function.Consumer<String> onCompleteCallback) {
        log.info("[InterviewerAgent] 开始流式合成, sessionId={}", sessionId);
        ConversationContext context = getOrCreateContext(sessionId);
        boolean fullVoice = isFullVoiceMode(sessionId);
        // full_voice 模式才创建 TTS 连接
        TTSService tts = fullVoice ? context.getOrCreateTTSService(() -> createTTSServiceForSession(sessionId)) : null;
        StringBuilder fullText = new StringBuilder();
        textStream.subscribe(
                delta -> {
                    // full_voice 模式提交 delta 到 TTS
                    if (fullVoice && tts != null) {
                        log.debug("[InterviewerAgent] 提交 delta, sessionId={}, delta={}", sessionId, delta);
                        tts.synthesize(delta, false);
                    }
                    fullText.append(delta);
                    // 每个 delta 直接发送转录到客户端（两种模式都需要）
                    voiceGateway.sendResponse(sessionId, VoiceResponse.transcript(
                            VoiceResponse.TranscriptData.builder()
                                    .text(delta)
                                    .isFinal(false)
                                    .role(TranscriptRole.INTERVIEWER.getValue())
                                    .build()
                    ));
                },
                error -> log.error("[InterviewerAgent] LLM流错误, sessionId={}", sessionId, error),
                () -> {
                    // LLM 流结束，刷新 TTS 缓冲区中的残余文本
                    if (fullVoice && tts != null) {
                        tts.synthesize("", true);
                    }
                    // 发送 final 标记
                    voiceGateway.sendResponse(sessionId, VoiceResponse.transcript(
                            VoiceResponse.TranscriptData.builder()
                                    .text("")
                                    .isFinal(true)
                                    .role(TranscriptRole.INTERVIEWER.getValue())
                                    .build()
                    ));
                    if (!fullText.isEmpty()) {
                        log.info("[InterviewerAgent] 流式合成完成, sessionId={}, textLength={}", sessionId, fullText.length());
                        context.addInterviewerMessage(fullText.toString());
                        // 完成回调（用于记录 lastQuestion 等）
                        if (onCompleteCallback != null) {
                            onCompleteCallback.accept(fullText.toString());
                        }
                    } else {
                        log.warn("[InterviewerAgent] 流式合成完成但文本为空, sessionId={}", sessionId);
                    }
                }
        );
    }

    /**
     * 判断是否为全语音模式（面试官也播放语音）
     */
    private boolean isFullVoiceMode(String sessionId) {
        SessionState state = voiceGateway.getInternalState(sessionId);
        return state != null && "full_voice".equals(state.getVoiceMode());
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
                ctx.setCompanyName(state.getCompanyName());
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
        // 通知前端题号更新
        voiceGateway.sendCurrentState(sessionId, InterviewSessionState.INTERVIEWING.getCode());
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
        log.info("[InterviewerAgent] 所有问题完成，结束面试, sessionId={}", sessionId);
        SessionState state = voiceGateway.getInternalState(sessionId);
        if (state == null) {
            log.warn("[InterviewerAgent] 会话不存在, sessionId={}", sessionId);
            return;
        }
        // 生成结束语并语音播报
        String closingText = "好的，今天的面试就到这里。感谢你的时间，我们会尽快联系你。";
        log.info("[InterviewerAgent] 面试结束: {}, sessionId={}", closingText, sessionId);
        recordInterviewerMessage(sessionId, closingText);
        synthesizeAndSend(sessionId, closingText, true);
        // 委托 Gateway 完成后续收尾：保存结果 + 触发复盘分析 + 通知前端 + 清理资源
        voiceGateway.endInterview(sessionId);
    }

    /**
     * 拼接反应文本和追问问题
     * 反应文本末尾如果不是标点符号，自动补逗号
     */
    private String buildReplyWithReaction(String reaction, String followUp) {
        if (reaction == null || reaction.isBlank()) {
            return followUp;
        }
        char last = reaction.charAt(reaction.length() - 1);
        if (last == '。' || last == '，' || last == '？' || last == '、') {
            return reaction + followUp;
        }
        return reaction + "，" + followUp;
    }

    /**
     * 构建追问后的过渡回复（根据面试官风格）
     */
    private String buildFollowUpTransition(String sessionId) {
        SessionState state = requireSession(sessionId);
        String style = state.getInterviewerStyle();
        if ("friendly".equals(style)) {
            return "好的，这部分聊得很清楚。我们换个话题吧。";
        } else if ("challenging".equals(style)) {
            return "嗯，好。继续下一个问题。";
        }
        // professional（默认）
        return "好的，了解了。我们继续下一个话题。";
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
