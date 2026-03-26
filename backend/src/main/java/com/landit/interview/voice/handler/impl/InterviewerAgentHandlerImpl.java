package com.landit.interview.voice.handler.impl;

import com.landit.common.config.VoiceProperties;
import com.landit.interview.voice.dto.*;
import com.landit.interview.voice.gateway.impl.InterviewVoiceGatewayImpl;
import com.landit.interview.voice.handler.InterviewerAgentHandler;
import com.landit.interview.voice.service.ASRService;
import com.landit.interview.voice.service.TTSService;
import com.landit.interview.voice.service.VoiceServiceFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

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
    private final InterviewVoiceGatewayImpl voiceGateway;

    // 会话上下文：sessionId -> ConversationContext
    private final ConcurrentHashMap<String, ConversationContext> contexts = new ConcurrentHashMap<>();

    @Override
    public Flux<VoiceResponse> handleCandidateAudio(String sessionId, byte[] audioData) {
        log.debug("[InterviewerAgent] Handling candidate audio, sessionId={}, size={}", sessionId, audioData.length);

        // 获取 ASR 服务
        ASRService asrService = voiceServiceFactory.getASRService();

        // 创建 ASR 配置
        ASRConfig asrConfig = buildASRConfig();

        // 获取会话上下文
        ConversationContext context = contexts.computeIfAbsent(sessionId, k -> new ConversationContext());

        // Step 1: ASR 识别
        return asrService.streamRecognize(Flux.just(audioData), asrConfig)
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

                    // 如果是最终结果，生成面试官回复
                    if (asrResult.getIsFinal() && !asrResult.getText().isEmpty()) {
                        context.addCandidateMessage(asrResult.getText());
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

        ConversationContext context = contexts.computeIfAbsent(sessionId, k -> new ConversationContext());
        InterviewVoiceGatewayImpl.SessionState state = voiceGateway.getInternalState(sessionId);

        // 构建生成问题的提示词
        String systemPrompt = buildInterviewerSystemPrompt();
        String userPrompt = String.format(
                "请生成第 %d 个面试问题（共 %d 个）。" +
                        "面试岗位：%s。" +
                        "之前的对话：%s",
                state.getCurrentQuestion() + 1,
                state.getTotalQuestions(),
                context.getPosition(),
                context.getConversationSummary()
        );

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

        // 构建提示词
        String systemPrompt = buildInterviewerSystemPrompt();
        String userPrompt = String.format(
                "候选人回答：%s\n\n" +
                        "请根据候选人的回答，给出评价和追问（如果有）。" +
                        "回答要简洁专业，适合口语表达。",
                candidateAnswer
        );

        // 调用 LLM 生成回复
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
                })
                .doOnComplete(() -> {
                    // 更新问题计数
                    InterviewVoiceGatewayImpl.SessionState state = voiceGateway.getInternalState(sessionId);
                    if (state != null) {
                        state.setCurrentQuestion(state.getCurrentQuestion() + 1);
                    }
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
     * 构建面试官系统提示词
     */
    private String buildInterviewerSystemPrompt() {
        return """
                你是一位经验丰富的技术面试官。你的职责是：
                1. 提出专业的技术问题
                2. 评估候选人的回答
                3. 根据回答进行适当的追问
                4. 给出简洁、专业的反馈

                你的回复要求：
                - 语言简洁，适合口语表达
                - 专业但不失亲和
                - 适时给予鼓励
                - 控制在 50-100 字之间
                """;
    }

    /**
     * 构建 ASR 配置
     */
    private ASRConfig buildASRConfig() {
        VoiceProperties.AliyunConfig.ASRConfig asrConfig = voiceProperties.getAliyun().getAsr();
        return ASRConfig.builder()
                .model(asrConfig.getModel())
                .format(asrConfig.getFormat())
                .sampleRate(asrConfig.getSampleRate())
                .enablePunctuation(asrConfig.getEnablePunctuation())
                .enableItn(asrConfig.getEnableItn())
                .language(asrConfig.getLanguage())
                .enableVad(asrConfig.getEnableVad())
                .build();
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
     * 对话上下文
     */
    @lombok.Data
    private static class ConversationContext {
        private String position = "Java 开发工程师";
        private StringBuilder conversationHistory = new StringBuilder();

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
    }
}
