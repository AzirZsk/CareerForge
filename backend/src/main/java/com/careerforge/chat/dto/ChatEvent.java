package com.careerforge.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SSE聊天事件
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatEvent {

    /**
     * 事件类型：chunk / suggestion / complete / error
     */
    private String type;

    /**
     * 事件内容（chunk时为String，suggestion时为List<SectionChange>）
     */
    private Object content;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 创建文本片段事件
     */
    public static ChatEvent chunk(String text) {
        return ChatEvent.builder()
                .type("chunk")
                .content(text)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建修改建议事件
     */
    public static ChatEvent suggestion(List<SectionChange> changes) {
        return ChatEvent.builder()
                .type("suggestion")
                .content(changes)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建完成事件
     */
    public static ChatEvent complete(String message) {
        return ChatEvent.builder()
                .type("complete")
                .content(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建错误事件
     */
    public static ChatEvent error(String message) {
        return ChatEvent.builder()
                .type("error")
                .content(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建简历选择事件
     * AI选择简历后通知前端
     *
     * @param resumeId   简历ID
     * @param resumeName 简历名称
     */
    public static ChatEvent resumeSelected(String resumeId, String resumeName) {
        return ChatEvent.builder()
                .type("resume_selected")
                .content(ResumeSelectedContent.of(resumeId, resumeName))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 简历选择事件内容
     */
    @Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ResumeSelectedContent {
        private String resumeId;
        private String resumeName;

        public static ResumeSelectedContent of(String resumeId, String resumeName) {
            return new ResumeSelectedContent(resumeId, resumeName);
        }
    }
}
