package com.landit.interview.voice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.common.config.VoiceProperties;
import lombok.extern.slf4j.Slf4j;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 阿里云语音服务基类
 * 提取公共的签名生成、URL 构建等方法
 *
 * @author Azir
 */
@Slf4j
public abstract class AliyunVoiceBaseService {

    protected static final String WS_URL_TEMPLATE = "wss://dashscope.aliyuncs.com/api-ws/v1/inference/%s?%s";

    protected final VoiceProperties voiceProperties;
    protected final ObjectMapper objectMapper;

    protected AliyunVoiceBaseService(VoiceProperties voiceProperties, ObjectMapper objectMapper) {
        this.voiceProperties = voiceProperties;
        this.objectMapper = objectMapper;
    }

    /** 检查 API Key 是否可用 */
    protected boolean isApiKeyAvailable() {
        VoiceProperties.AliyunConfig aliyun = voiceProperties.getAliyun();
        return aliyun != null && aliyun.getApiKey() != null && !aliyun.getApiKey().isBlank();
    }

    /** 获取 API Key */
    protected String getApiKey() {
        VoiceProperties.AliyunConfig aliyun = voiceProperties.getAliyun();
        if (aliyun == null || aliyun.getApiKey() == null || aliyun.getApiKey().isBlank()) {
            throw new IllegalStateException("Aliyun API key not configured");
        }
        return aliyun.getApiKey();
    }

    /** 生成 WebSocket 签名 URL */
    protected String buildSignedUrl(String model) throws Exception {
        String apiKey = getApiKey();

        long timestamp = System.currentTimeMillis();
        String nonce = UUID.randomUUID().toString().replace("-", "");

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("model=").append(urlEncode(model));
        queryBuilder.append("&timestamp=").append(timestamp);
        queryBuilder.append("&nonce=").append(nonce);

        String signature = generateSignature(apiKey, queryBuilder.toString());
        queryBuilder.append("&signature=").append(urlEncode(signature));

        return String.format(WS_URL_TEMPLATE, model, queryBuilder);
    }

    /** 生成 HmacSHA256 签名 */
    private String generateSignature(String apiKey, String query)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                apiKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(query.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    /** URL 编码 */
    protected String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    /** 生成任务 ID */
    protected String generateTaskId() {
        return UUID.randomUUID().toString();
    }

    /** JSON 字符串转义 */
    protected String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
