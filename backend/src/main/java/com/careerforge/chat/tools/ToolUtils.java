package com.careerforge.chat.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.careerforge.chat.dto.tool.ToolErrorResponse;
import com.careerforge.chat.dto.tool.ToolResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * AI工具类通用工具方法
 * 提供JSON序列化和错误响应构建功能
 *
 * @author Azir
 */
@Slf4j
public final class ToolUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ToolUtils() {
        // 工具类不允许实例化
    }

    /**
     * 将ToolResponse序列化为JSON字符串
     *
     * @param response 响应对象
     * @return JSON字符串
     */
    public static String toJson(ToolResponse response) {
        try {
            return OBJECT_MAPPER.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.error("[ToolUtils] JSON序列化失败", e);
            return "{\"success\": false, \"error\": \"JSON序列化失败\"}";
        }
    }

    /**
     * 创建带简历ID的错误响应JSON
     *
     * @param message  错误信息
     * @param resumeId 简历ID
     * @return JSON字符串
     */
    public static String errorResponse(String message, String resumeId) {
        return toJson(ToolErrorResponse.of(message, resumeId));
    }

    /**
     * 创建带区块ID的错误响应JSON
     *
     * @param message   错误信息
     * @param sectionId 区块ID
     * @return JSON字符串
     */
    public static String errorResponseWithSection(String message, String sectionId) {
        return toJson(ToolErrorResponse.ofSection(message, sectionId));
    }

    /**
     * 创建简单错误响应JSON
     *
     * @param message 错误信息
     * @return JSON字符串
     */
    public static String errorResponse(String message) {
        return toJson(new ToolErrorResponse(message));
    }
}
