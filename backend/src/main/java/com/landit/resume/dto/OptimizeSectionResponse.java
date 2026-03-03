package com.landit.resume.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.landit.common.annotation.SchemaField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 简历内容优化响应DTO
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizeSectionResponse {

    /**
     * 变更记录
     */
    @SchemaField(value = "内容变更记录列表")
    private List<Change> changes;

    /**
     * 补充提示
     */
    @SchemaField(value = "优化相关的补充提示")
    private List<String> tips;

    /**
     * 预估提升分数
     */
    @SchemaField(value = "预估的简历提升分数(0-100)")
    private Integer improvementScore;

    /**
     * 变更记录
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Change {
        /**
         * 变更类型：added/modified/removed
         */
        @SchemaField(value = "变更类型", enumValues = {"added", "modified", "removed"})
        private String type;

        /**
         * 变更类型中文翻译
         */
        private String typeLabel;

        /**
         * 字段路径
         * 示例：basicInfo.name, education[0].school, work[0].description
         */
        @SchemaField(value = "变更字段的路径，如 basicInfo.name 或 education[0].school")
        private String field;

        /**
         * 字段路径中文翻译
         */
        private String fieldLabel;

        /**
         * 修改前（支持字符串、数组、对象等多种类型）
         */
        @SchemaField(value = "修改前的内容")
        private Object before;

        /**
         * 修改后（支持字符串、数组、对象等多种类型）
         */
        @SchemaField(value = "修改后的内容")
        private Object after;

        /**
         * 修改原因
         */
        @SchemaField(value = "修改原因说明")
        private String reason;
    }
}
