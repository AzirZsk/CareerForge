package com.landit.resume.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 模块内容优化响应DTO
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizeSectionResponse {

    /**
     * 优化后的内容
     */
    private Map<String, Object> optimizedContent;

    /**
     * 变更记录
     */
    private List<Change> changes;

    /**
     * 补充提示
     */
    private List<String> tips;

    /**
     * 置信度：high/medium/low
     */
    private String confidence;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Change {
        /**
         * 变更类型：added/modified/removed
         */
        private String type;

        /**
         * 字段路径
         */
        private String field;

        /**
         * 修改前
         */
        private String before;

        /**
         * 修改后
         */
        private String after;

        /**
         * 修改原因
         */
        private String reason;
    }

}
