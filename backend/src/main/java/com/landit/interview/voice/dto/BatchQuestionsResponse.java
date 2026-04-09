package com.landit.interview.voice.dto;

import com.landit.common.annotation.SchemaField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量生成面试问题响应 DTO
 * 用于接收 LLM 结构化返回的面试问题列表
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchQuestionsResponse {

    /**
     * 面试问题列表
     */
    @SchemaField(value = "面试问题列表")
    private List<QuestionItem> questions;

    /**
     * 单个面试问题
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionItem {

        /**
         * 面试问题文本
         */
        @SchemaField(value = "面试问题文本，口语化表达，20-40字")
        private String text;
    }
}
