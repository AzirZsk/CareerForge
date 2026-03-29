package com.landit.interview.voice.service;

import com.landit.common.config.VoiceProperties;
import com.landit.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 语音服务工厂
 * 根据配置获取对应的 ASR/TTS 服务实例
 *
 * <p>支持的服务商：aliyun, openai, azure
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceServiceFactory {

    private final VoiceProperties voiceProperties;
    private final Map<String, ASRService> asrServices;
    private final Map<String, TTSService> ttsServices;

    /**
     * 通过构造注入自动收集所有 ASRService 实现
     */
    public VoiceServiceFactory(
            VoiceProperties voiceProperties,
            List<ASRService> asrServiceList,
            List<TTSService> ttsServiceList
    ) {
        this.voiceProperties = voiceProperties;
        this.asrServices = asrServiceList.stream()
                .collect(Collectors.toMap(ASRService::getProvider, Function.identity()));
        this.ttsServices = ttsServiceList.stream()
                .collect(Collectors.toMap(TTSService::getProvider, Function.identity()));

        log.info("[VoiceServiceFactory] Initialized with providers - ASR: {}, TTS: {}",
                asrServices.keySet(), ttsServices.keySet());
    }

    /**
     * 获取当前配置的 ASR 服务
     *
     * @return ASR 服务实例
     * @throws BusinessException 如果服务不可用
     */
    public ASRService getASRService() {
        String provider = voiceProperties.getProvider();
        return getASRService(provider);
    }

    /**
     * 获取指定提供商的 ASR 服务
     *
     * @param provider 提供商标识：aliyun, openai, azure
     * @return ASR 服务实例
     * @throws BusinessException 如果服务不可用
     */
    public ASRService getASRService(String provider) {
        ASRService service = asrServices.get(provider);
        if (service == null) {
            throw new BusinessException("不支持的 ASR 服务提供商: " + provider +
                    "，支持的提供商: " + asrServices.keySet());
        }
        if (!service.isAvailable()) {
            throw new BusinessException("ASR 服务不可用，请检查配置: " + provider);
        }
        return service;
    }

    /**
     * 获取当前配置的 TTS 服务
     *
     * @return TTS 服务实例
     * @throws BusinessException 如果服务不可用
     */
    public TTSService getTTSService() {
        String provider = voiceProperties.getProvider();
        return getTTSService(provider);
    }

    /**
     * 获取指定提供商的 TTS 服务
     *
     * @param provider 提供商标识：aliyun, openai, azure
     * @return TTS 服务实例
     * @throws BusinessException 如果服务不可用
     */
    public TTSService getTTSService(String provider) {
        TTSService service = ttsServices.get(provider);
        if (service == null) {
            throw new BusinessException("不支持的 TTS 服务提供商: " + provider +
                    "，支持的提供商: " + ttsServices.keySet());
        }
        if (!service.isAvailable()) {
            throw new BusinessException("TTS 服务不可用，请检查配置: " + provider);
        }
        return service;
    }

    /**
     * 获取当前配置的提供商
     *
     * @return 提供商标识
     */
    public String getCurrentProvider() {
        return voiceProperties.getProvider();
    }

    /**
     * 检查 ASR 服务是否可用
     *
     * @return true 表示可用
     */
    public boolean isASRAvailable() {
        try {
            ASRService service = asrServices.get(voiceProperties.getProvider());
            return service != null && service.isAvailable();
        } catch (Exception e) {
            log.warn("[VoiceServiceFactory] Failed to check ASR availability", e);
            return false;
        }
    }

    /**
     * 检查 TTS 服务是否可用
     *
     * @return true 表示可用
     */
    public boolean isTTSAvailable() {
        try {
            TTSService service = ttsServices.get(voiceProperties.getProvider());
            return service != null && service.isAvailable();
        } catch (Exception e) {
            log.warn("[VoiceServiceFactory] Failed to check TTS availability", e);
            return false;
        }
    }

    /**
     * 检查语音服务是否完全可用（ASR + TTS）
     *
     * @return true 表示全部可用
     */
    public boolean isVoiceServiceAvailable() {
        return isASRAvailable() && isTTSAvailable();
    }
}
