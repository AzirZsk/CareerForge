package com.landit.interview.dto;

import com.landit.common.annotation.SchemaField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 面试准备项 DTO
 * AI 生成准备事项时返回的结构化数据
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreparationItemResult {

    /**
     * 准备项类型
     */
    @SchemaField(value = "准备项类型（company_research/jd_keywords/tech_prep/behavioral/case_study/todo）", required = true)
    private String itemType;

    /**
     * 标题
     */
    @SchemaField(value = "标题（简洁明了，不超过50字）", required = true)
    private String title;

    /**
     * 具体内容
     */
    @SchemaField(value = "具体内容（详细说明需要准备什么，50-200字）", required = true)
    private String content;

    /**
     * 优先级
     */
    @SchemaField(value = "优先级（required/recommended/optional）", required = true)
    private String priority;

    /**
     * 关联资源列表
     */
    @SchemaField(value = "关联资源列表（可选）")
    private List<PreparationResource> resources;

    /**
     * 准备资源
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreparationResource {

        /**
         * 资源类型
         */
        @SchemaField(value = "资源类型（link/note/video）", required = true)
        private String type;

        /**
         * 资源标题
         */
        @SchemaField(value = "资源标题", required = true)
        private String title;

        /**
         * 链接地址
         */
        @SchemaField(value = "链接地址（type为link时必填）")
        private String url;

    }

}
