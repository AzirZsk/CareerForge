package com.careerforge.interview.voice.config;

import com.careerforge.common.config.VoiceProperties;
import com.careerforge.interview.voice.enums.VoiceRole;
import com.careerforge.interview.voice.service.TTSService;
import com.careerforge.interview.voice.service.impl.AliyunTTSService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云语音服务条件装配配置
 * 仅在 careerforge.voice.provider=aliyun 时激活
 *
 * <p>ASR 服务由 VoiceServiceFactory.createASRService() 按需创建（会话级实例）
 * <p>TTS 服务注册为单例 Bean
 *
 * @author Azir
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "careerforge.voice.provider", havingValue = "aliyun")
public class AliyunVoiceAutoConfiguration {

    /**
     * 配置阿里云 TTS 服务（单例）
     */
    @Bean
    @ConditionalOnMissingBean(TTSService.class)
    public TTSService aliyunTTSService(VoiceProperties voiceProperties) {
        log.info("[AliyunVoiceAutoConfiguration] 正在初始化阿里云TTS服务");
        return new AliyunTTSService(voiceProperties, VoiceRole.INTERVIEWER);
    }
}
