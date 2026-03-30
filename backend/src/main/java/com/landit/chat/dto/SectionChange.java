package com.landit.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 区块变更明细
 * 用于表示AI建议的简历修改内容
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionChange {

    /**
     * 区块ID（update/delete时需要）
     */
    private String sectionId;

    /**
     * 区块类型（add时需要）：BASIC_INFO/EDUCATION/WORK/PROJECT/SKILLS/CERTIFICATE/OPEN_SOURCE/CUSTOM
     */
    private String sectionType;

    /**
     * 区块标题（显示用）
     */
    private String sectionTitle;

    /**
     * 变更类型：update / add / delete
     */
    private String changeType;

    /**
     * 修改前的内容（JSON格式，前端填充）
     */
    private String beforeContent;

    /**
     * 修改后的内容（JSON格式）
     */
    private String afterContent;

    /**
     * 修改说明
     */
    private String description;

    /**
     * 变更值（用于详细对比展示）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangeValue {
        private String stringValue;
        private List<String> arrayValue;
    }

    /**
     * 变更字段（用于详细对比展示）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldChange {
        /**
         * 字段路径：work[0].description
         */
        private String field;

        /**
         * 值类型：string / string_array
         */
        private String valueType;

        /**
         * 修改前的值
         */
        private ChangeValue before;

        /**
         * 修改后的值
         */
        private ChangeValue after;

        /**
         * 修改原因
         */
        private String reason;
    }
}
