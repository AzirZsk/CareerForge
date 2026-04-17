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
import com.careerforge.interview.voice.service.VoiceServiceFactory;
import com.careerforge.interview.voice.service.impl.AliyunTTSService;
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
import java.util.concurrent.ConcurrentHashMap;

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
    private final ChatClient chatClient;
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
     * 音频帧通过 ASR 会话汇聚到同一条 WebSocket 连接，识别结果通过回调处理
     *
     * @param sessionId 会话 ID
     * @param audioData 音频数据（PCM）
     * @return 空流（结果通过 voiceGateway.sendResponse 回调推送）
     */
    public Flux<VoiceResponse> handleCandidateAudio(String sessionId, byte[] audioData) {
        log.debug("[InterviewerAgent] 处理候选人音频, sessionId={}, size={}", sessionId, audioData.length);

        // 获取会话上下文（从 Gateway 加载 JD/简历）
        ConversationContext context = getOrCreateContext(sessionId);

        // 收集候选人音频数据（用于录音保存）
        context.appendCandidateAudio(audioData);

        // 获取或创建 ASR 会话（首次创建时启动并注册回调）
        ASRService asr = context.getOrCreateASRService(() -> {
            ASRService service = voiceServiceFactory.createASRService();
            // 启动连接并注册回调，识别结果通过 ASRListener 异步回调
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

        return Flux.empty();
    }

    /**
     * 处理 ASR 识别结果
     * 发送转录结果给客户端，isFinal 时触发面试逻辑
     *
     * @param sessionId 会话 ID
     * @param asrResult ASR 识别结果
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

            // 根据面试阶段处理（通过 subscribe 驱动，结果通过 sendResponse 推送）
            handleCandidateTranscriptByPhase(sessionId, asrResult.getText())
                    .subscribe(
                            response -> voiceGateway.sendResponse(sessionId, response),
                            error -> log.error("[InterviewerAgent] 阶段处理错误, sessionId={}", sessionId, error)
                    );
        }
    }

    /**
     * 清理会话资源（关闭 ASR 连接、移除上下文）
     *
     * @param sessionId 会话 ID
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
            AliyunTTSService tts = context.getTtsService();
            if (tts != null) {
                try {
                    tts.closeConnection();
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
            state.setPhase(InterviewPhaseEnum.ASKING_QUESTIONS);
            return advanceToNextQuestion(sessionId, state);

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
     * 请求自我介绍
     *
     * @param sessionId 会话 ID
     * @return 响应流（自我介绍请求文本 + 音频）
     */
    public Flux<VoiceResponse> requestSelfIntroduction(String sessionId) {
        log.debug("[InterviewerAgent] 请求自我介绍, sessionId={}", sessionId);

        SessionState state = requireSession(sessionId);

        // 获取面试官风格配置
        AIPromptProperties.InterviewerStyleConfig styleConfig =
                aiPromptProperties.getVoice().getByStyle(state.getInterviewerStyle());

        // 获取自我介绍请求文本
        String prompt = styleConfig.getSelfIntroductionPromptTemplate();
        if (prompt == null || prompt.isBlank()) {
            prompt = "请先做个自我介绍吧，说说你的技术背景和项目经验。";
        }

        log.info("[InterviewerAgent] 请求自我介绍: {}, sessionId={}", prompt, sessionId);

        // 记录面试官消息到对话历史
        recordInterviewerMessage(sessionId, prompt);

        return synthesizeToVoiceResponses(sessionId, prompt);
    }

    /**
     * 生成下一个问题（优先使用预生成问题）
     *
     * @param sessionId 会话 ID
     * @return 响应流（问题文本 + 音频）
     */
    public Flux<VoiceResponse> generateNextQuestion(String sessionId) {
        log.debug("[InterviewerAgent] 生成下一个问题, sessionId={}", sessionId);

        SessionState state = requireSession(sessionId);

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

            // 记录面试官消息到对话历史
            recordInterviewerMessage(sessionId, questionText);

            return synthesizeToVoiceResponses(sessionId, questionText);
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
        ConversationContext context = getOrCreateContext(sessionId);

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

        // 累积完整问题文本用于记录对话历史
        StringBuilder fullQuestion = new StringBuilder();
        return synthesizeStreamToVoiceResponses(sessionId, llmStream)
                .doOnNext(resp -> {
                    // 从 transcript 响应中提取文本累积
                    if (resp.getData() instanceof VoiceResponse.TranscriptData data) {
                        fullQuestion.append(data.getText());
                    }
                })
                .doOnComplete(() -> {
                    if (fullQuestion.length() > 0) {
                        context.addInterviewerMessage(fullQuestion.toString());
                    }
                });
    }

    /**
     * 生成面试官回复（含追问判断）
     */
    private Flux<VoiceResponse> generateInterviewerReply(String sessionId, String candidateAnswer) {
        log.debug("[InterviewerAgent] 生成面试官回复, sessionId={}", sessionId);

        SessionState state = requireSession(sessionId);

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

            // 记录面试官追问到对话历史
            recordInterviewerMessage(sessionId, followUp);

            return synthesizeToVoiceResponses(sessionId, followUp);
        } else {
            // 不追问，直接进入下一个问题
            return advanceToNextQuestion(sessionId, state);
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
     * 获取完整对话历史（面试结束时使用）
     *
     * @param sessionId 会话 ID
     * @return 完整对话文本
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
        return chatClient
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .stream()
                .content();
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
     * 将文本通过 TTS 分句流式合成，转换为 VoiceResponse 流
     * 使用 session-scoped TTS 连接复用，整场面试共享一条 WebSocket 连接
     *
     * @param sessionId 会话 ID
     * @param text      要合成的文本
     * @return VoiceResponse 流（交替包含 transcript 和 audio）
     */
    private Flux<VoiceResponse> synthesizeToVoiceResponses(String sessionId, String text) {
        ConversationContext context = getOrCreateContext(sessionId);
        TTSConfig ttsConfig = buildInterviewerTTSConfig();
        AliyunTTSService tts = context.getOrCreateTTSService(() -> {
            AliyunTTSService service = voiceServiceFactory.createTTSService(ttsConfig);
            service.connect();
            return service;
        });
        return tts.streamSynthesizeBySentenceWithConnection(Flux.just(text))
                .flatMap(chunk -> buildTTSResponseFlux(chunk, ttsConfig));
    }

    /**
     * 将 LLM 文本流通过 TTS 分句流式合成，转换为 VoiceResponse 流
     * 使用 session-scoped TTS 连接复用，用于实时生成问题的场景
     *
     * @param sessionId  会话 ID
     * @param textStream LLM 文本流
     * @return VoiceResponse 流（交替包含 transcript 和 audio）
     */
    private Flux<VoiceResponse> synthesizeStreamToVoiceResponses(String sessionId, Flux<String> textStream) {
        ConversationContext context = getOrCreateContext(sessionId);
        TTSConfig ttsConfig = buildInterviewerTTSConfig();
        AliyunTTSService tts = context.getOrCreateTTSService(() -> {
            AliyunTTSService service = voiceServiceFactory.createTTSService(ttsConfig);
            service.connect();
            return service;
        });
        return tts.streamSynthesizeBySentenceWithConnection(textStream)
                .flatMap(chunk -> buildTTSResponseFlux(chunk, ttsConfig));
    }

    /**
     * 将单个 TTSChunk 转换为 VoiceResponse 流
     * 有音频时输出 transcript + audio，无音频时仅输出 transcript
     */
    private Flux<VoiceResponse> buildTTSResponseFlux(TTSChunk ttsChunk, TTSConfig ttsConfig) {
        Flux<VoiceResponse> textFlux = Flux.just(VoiceResponse.transcript(
                VoiceResponse.TranscriptData.builder()
                        .text(ttsChunk.getText())
                        .isFinal(ttsChunk.getIsFinal())
                        .role(TranscriptRole.INTERVIEWER.getValue())
                        .build()
        ));
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
    }

    /**
     * 获取或创建会话上下文
     * 首次创建时从 Gateway 加载 JD/简历信息
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
     * 获取必须存在的会话状态，不存在则抛出异常
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
     * 重置追问状态，更新问题计数，根据剩余问题数决定下一步
     *
     * @param sessionId 会话 ID
     * @param state     会话状态
     * @return 响应流（下一个问题或结束语）
     */
    private Flux<VoiceResponse> advanceToNextQuestion(String sessionId, SessionState state) {
        state.setFollowUpCount(0);
        state.setCurrentQuestion(state.getCurrentQuestion() + 1);

        if (state.getCurrentQuestion() < state.getTotalQuestions()) {
            log.info("[InterviewerAgent] 进入下一个问题, sessionId={}, index={}", sessionId, state.getCurrentQuestion());
            return generateNextQuestion(sessionId);
        }
        log.info("[InterviewerAgent] 所有问题完成，结束面试, sessionId={}", sessionId);
        return endInterview(sessionId);
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
        state.complete();

        // 生成结束语
        String closingText = "好的，今天的面试就到这里。感谢你的时间，我们会尽快联系你。";
        log.info("[InterviewerAgent] 面试结束: {}, sessionId={}", closingText, sessionId);

        // 记录结束语到对话历史
        recordInterviewerMessage(sessionId, closingText);

        return synthesizeToVoiceResponses(sessionId, closingText);
    }
}
