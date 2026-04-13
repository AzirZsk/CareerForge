package com.careerforge.resume.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 简历模块优化上下文
 * 用于 OptimizeSectionNode 构建传递给 AI 的数据结构
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizeSectionContext {

    /**
     * 简历模块 ID
     */
    private String sectionId;

    /**
     * 模块类型（如 WORK, PROJECT 等）
     */
    private String type;

    /**
     * 模块内容（JSON 字符串）
     */
    private String content;

    /**
     * 该模块对应的优化策略列表
     */
    private List<SimplifiedStrategy> strategies;

    /**
     * 简化的优化策略
     * 仅包含 AI 优化所需的核心字段
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimplifiedStrategy {
        /**
         * 建议标题
         */
        private String title;

        /**
         * 问题描述
         */
        private String problem;

        /**
         * 改进方向
         */
        private String direction;

        /**
         * 改进示例
         */
        private String example;
    }
}
