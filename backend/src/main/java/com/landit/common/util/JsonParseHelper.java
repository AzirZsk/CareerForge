package com.landit.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON 解析工具类
 * 提供 JSON 提取和解析的通用方法
 *
 * @author Azir
 */
@Slf4j
public final class JsonParseHelper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonParseHelper() {
        // 工具类禁止实例化
    }

    /**
     * 从文本中提取 JSON 对象
     * 自动定位第一个 { 和最后一个 } 之间的内容
     *
     * @param text 包含 JSON 的文本
     * @return 提取的 JSON 字符串，提取失败返回 "{}"
     */
    public static String extractJson(String text) {
        if (text == null || text.isEmpty()) {
            return "{}";
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return "{}";
    }

    /**
     * 将 JSON 字符串解析为 Map
     *
     * @param json JSON 字符串
     * @return 解析后的 Map，解析失败返回空 Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseToMap(String json) {
        try {
            String cleanJson = extractJson(json);
            return OBJECT_MAPPER.readValue(cleanJson, Map.class);
        } catch (Exception e) {
            log.error("解析JSON失败: {}", json, e);
            return new HashMap<>();
        }
    }

    /**
     * 将对象转换为 JSON 字符串
     *
     * @param obj 要转换的对象
     * @return JSON 字符串，转换失败返回 "{}"
     */
    public static String toJsonString(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("对象转JSON失败", e);
            return "{}";
        }
    }

    /**
     * 清理 JSON 响应（移除 markdown 代码块标记等）
     *
     * @param response 原始响应
     * @return 清理后的 JSON
     */
    public static String cleanJsonResponse(String response) {
        if (response == null) {
            return "{}";
        }
        String cleaned = response.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    /**
     * 获取默认的诊断结果结构
     *
     * @return 包含默认值的诊断结果 Map
     */
    public static Map<String, Object> getDefaultDiagnosisResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("overallScore", 0);
        result.put("matchScore", 0);
        result.put("dimensions", new HashMap<>());
        result.put("issues", new ArrayList<>());
        result.put("highlights", new ArrayList<>());
        result.put("quickWins", new ArrayList<>());
        result.put("marketRequirements", new HashMap<>());
        result.put("skillMatch", new HashMap<>());
        return result;
    }

    /**
     * 获取默认的建议结果结构
     *
     * @return 包含默认值的建议结果 Map
     */
    public static Map<String, Object> getDefaultSuggestionsResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("suggestions", new ArrayList<>());
        result.put("quickWins", new ArrayList<>());
        result.put("priorityOrder", new ArrayList<>());
        result.put("estimatedImprovement", "无法估算");
        return result;
    }

    /**
     * 获取默认的优化结果结构
     *
     * @return 包含默认值的优化结果 Map
     */
    public static Map<String, Object> getDefaultOptimizeResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("optimizedContent", new HashMap<>());
        result.put("changes", new ArrayList<>());
        result.put("improvementScore", 0);
        result.put("tips", new ArrayList<>());
        result.put("confidence", "low");
        return result;
    }

    /**
     * 构建节点输出数据（用于 SSE）
     *
     * @param node    节点名称
     * @param progress 进度百分比
     * @param message  进度消息
     * @param data     节点数据
     * @return 节点输出 Map
     */
    public static Map<String, Object> buildNodeOutput(String node, int progress, String message, Map<String, Object> data) {
        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put("node", node);
        nodeOutput.put("progress", progress);
        nodeOutput.put("message", message);
        nodeOutput.put("data", data);
        return nodeOutput;
    }
}
