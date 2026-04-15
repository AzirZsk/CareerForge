package com.careerforge.interview.voice.service;

import com.careerforge.common.config.VoiceProperties;
import com.careerforge.common.exception.BusinessException;
import com.careerforge.interview.voice.service.impl.AliyunASRService;
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
 * <p>ASR 服务：每次调用 createASRService() 创建新实例（会话级，非单例）
 * <p>TTS 服务：单例 Bean，通过构造注入收集
 *
 * @author Azir
 */
@Slf4j
@Component
public class VoiceServiceFactory {

    private final VoiceProperties voiceProperties;
    private final Map<String, TTSService> ttsServices;

    /**
     * 通过构造注入自动收集所有 TTSService 实现
     */
    public VoiceServiceFactory(
            VoiceProperties voiceProperties,
            List<TTSService> ttsServiceList
    ) {
        this.voiceProperties = voiceProperties;
        this.ttsServices = ttsServiceList.stream()
                .collect(Collectors.toMap(TTSService::getProvider, Function.identity()));

        log.info("[VoiceServiceFactory] 初始化完成 - TTS: {}", ttsServices.keySet());
    }

    /**
     * 创建一个新的 ASR 识别会话
     * 每个面试会话应调用一次，创建独立的 WebSocket 连接
     *
     * @return 新的 ASRService 实例（未启动）
     * @throws BusinessException 如果服务不可用
     */
    public ASRService createASRService() {
        if (!isASRAvailable()) {
            throw new BusinessException("ASR 服务不可用，请检查配置");
        }
        log.debug("[VoiceServiceFactory] 创建新的 ASR 会话");
        return new AliyunASRService(voiceProperties);
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
     * @param provider 提供商标识：aliyun
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
     * 检查 ASR 服务是否可用
     *
     * @return true 表示配置完整可用
     */
    public boolean isASRAvailable() {
        return AliyunASRService.isConfigured(voiceProperties);
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
            log.warn("[VoiceServiceFactory] 检查TTS可用性失败", e);
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
