package com.landit.chat.tools;

/**
 * AI 工具类通用工具方法
 *
 * @author Azir
 */
public final class ToolUtils {

    private ToolUtils() {
        // 工具类不允许实例化
    }

    /**
     * 转义字符串中的特殊字符，用于 JSON 字符串构建
     *
     * @param str 原始字符串
     * @return 转义后的字符串
     */
    public static String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * 构建成功响应 JSON
     *
     * @param data 数据内容
     * @return JSON 字符串
     */
    public static String successResponse(String data) {
        return "{\"success\": true, \"data\": " + data + "}";
    }

    /**
     * 构建错误响应 JSON
     *
     * @param message 错误信息
     * @return JSON 字符串
     */
    public static String errorResponse(String message) {
        return "{\"success\": false, \"error\": \"" + escapeJson(message) + "\"}";
    }
}
