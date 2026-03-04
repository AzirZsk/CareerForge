package com.landit.resume.dto;

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
     * 变更值容器
     * 用于明确区分字符串值和数组值，指导大模型输出规范格式
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangeValue {
        /**
         * 字符串值，当 valueType 为 string 时使用
         */
        @SchemaField("字符串值，当valueType为string时使用")
        private String stringValue;

        /**
         * 字符串数组值，当 valueType 为 string_array 时使用
         */
        @SchemaField("字符串数组值，当valueType为string_array时使用")
        private List<String> arrayValue;

        /**
         * 获取实际值
         * 优先返回数组值，如果数组为空则返回字符串值
         */
        public Object getActualValue() {
            if (arrayValue != null && !arrayValue.isEmpty()) {
                return arrayValue;
            }
            return stringValue;
        }

        /**
         * 创建字符串类型的 ChangeValue
         */
        public static ChangeValue ofString(String value) {
            return ChangeValue.builder().stringValue(value).build();
        }

        /**
         * 创建数组类型的 ChangeValue
         */
        public static ChangeValue ofArray(List<String> value) {
            return ChangeValue.builder().arrayValue(value).build();
        }
    }

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
         * 值类型，决定 before 和 after 的数据格式
         * - string: 字符串类型，使用 stringValue 字段
         * - string_array: 字符串数组类型，使用 arrayValue 字段
         */
        @SchemaField(value = "值类型，决定before和after的数据格式", enumValues = {"string", "string_array"})
        private String valueType;

        /**
         * 修改前的值
         */
        @SchemaField(value = "修改前的值")
        private ChangeValue before;

        /**
         * 修改后的值
         */
        @SchemaField(value = "修改后的值")
        private ChangeValue after;

        /**
         * 修改原因
         */
        @SchemaField(value = "修改原因说明")
        private String reason;

        /**
         * 获取修改前的实际值
         */
        public Object getBeforeValue() {
            return before != null ? before.getActualValue() : null;
        }

        /**
         * 获取修改后的实际值
         */
        public Object getAfterValue() {
            return after != null ? after.getActualValue() : null;
        }
    }
}
