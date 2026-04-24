package com.careerforge.interview.voice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * SSE 求助事件
 * 用于流式返回助手回复
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistSSEEvent {

    /**
     * 事件类型：text, structured, done, error
     */
    private String type;

    /**
     * 事件数据
     */
    private Object data;

    /**
     * 创建文本事件
     */
    public static AssistSSEEvent text(TextEventData data) {
        return AssistSSEEvent.builder().type("text").data(data).build();
    }

    /**
     * 创建结构化事件（解析后的求助内容）
     */
    public static AssistSSEEvent structured(StructuredEventData data) {
        return AssistSSEEvent.builder().type("structured").data(data).build();
    }

    /**
     * 创建完成事件
     */
    public static AssistSSEEvent done(DoneEventData data) {
        return AssistSSEEvent.builder().type("done").data(data).build();
    }

    /**
     * 创建错误事件
     */
    public static AssistSSEEvent error(ErrorEventData data) {
        return AssistSSEEvent.builder().type("error").data(data).build();
    }

    /**
     * 文本事件数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextEventData {
        /**
         * 文本内容（增量或完整）
         */
        private String content;

        /**
         * 是否增量（true=增量，false=完整）
         */
        private Boolean isDelta;
    }

    /**
     * 完成事件数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoneEventData {
        /**
         * 剩余求助次数
         */
        private Integer assistRemaining;

        /**
         * 总时长（毫秒）
         */
        private Integer totalDurationMs;
    }

    /**
     * 错误事件数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorEventData {
        /**
         * 错误码
         */
        private String code;

        /**
         * 错误消息
         */
        private String message;
    }

    /**
     * 结构化事件数据（AI 返回的 JSON 解析结果）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StructuredEventData {
        /**
         * 求助类型 code（give_hints/explain_concept/free_question）
         */
        private String assistType;

        /**
         * 结构化内容（解析后的 JSON 数据）
         */
        private Map<String, Object> content;
    }
}
