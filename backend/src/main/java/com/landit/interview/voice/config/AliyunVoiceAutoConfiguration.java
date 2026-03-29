package com.landit.interview.voice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.common.config.VoiceProperties;
import com.landit.interview.voice.service.ASRService;
import com.landit.interview.voice.service.TTSService;
import com.landit.interview.voice.service.impl.AliyunASRService;
import com.landit.interview.voice.service.impl.AliyunTTSService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云语音服务条件装配配置
 * 仅在 landit.voice.provider=aliyun 时激活
 *
 * <p>配置示例：
 * <pre>
 * landit:
 *   voice:
 *     provider: aliyun
 *     aliyun:
 *       api-key: ${ALIYUN_API_KEY:}
 *       asr:
 *         model: paraformer-realtime-v2
 *       tts:
 *         model: cosyvoice-v2
 * </pre>
 *
 * @author Azir
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "landit.voice.provider", havingValue = "aliyun")
public class AliyunVoiceAutoConfiguration {

    /**
     * 配置阿里云 ASR 服务
     */
    @Bean
    @ConditionalOnMissingBean(ASRService.class)
    public ASRService aliyunASRService(VoiceProperties voiceProperties, ObjectMapper objectMapper) {
        log.info("[AliyunVoiceAutoConfiguration] Initializing Aliyun ASR service");
        return new AliyunASRService(voiceProperties, objectMapper);
    }

    /**
     * 配置阿里云 TTS 服务
     */
    @Bean
    @ConditionalOnMissingBean(TTSService.class)
    public TTSService aliyunTTSService(VoiceProperties voiceProperties, ObjectMapper objectMapper) {
        log.info("[AliyunVoiceAutoConfiguration] Initializing Aliyun TTS service");
        return new AliyunTTSService(voiceProperties, objectMapper);
    }
}
