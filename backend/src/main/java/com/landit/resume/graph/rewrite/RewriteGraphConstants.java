package com.landit.resume.graph.rewrite;

import com.landit.resume.graph.BaseGraphConstants;

/**
 * 简历风格改写工作流常量定义
 * 统一管理 Graph 相关的节点名称、状态键等字符串常量
 *
 * @author Azir
 */
public final class RewriteGraphConstants extends BaseGraphConstants {

    private RewriteGraphConstants() {
    }

    // ==================== Graph 名称 ====================

    /**
     * 简历风格改写工作流 Graph 名称
     */
    public static final String GRAPH_RESUME_REWRITE = "resume_rewrite";

    // ==================== 节点名称 ====================

    /**
     * 分析参考简历风格节点
     */
    public static final String NODE_ANALYZE_STYLE = "analyze_style";

    /**
     * 生成风格差异建议节点
     */
    public static final String NODE_GENERATE_STYLE_DIFF = "generate_style_diff";

    /**
     * 应用风格改写节点
     */
    public static final String NODE_REWRITE_SECTION = "rewrite_section";

    // ==================== 状态键 - 参考简历（特有） ====================

    /**
     * 参考简历内容（ResumeDetailVO）
     */
    public static final String STATE_REFERENCE_CONTENT = "reference_content";

    // ==================== 状态键 - 风格分析（特有） ====================

    /**
     * 风格分析结果（JSON格式）
     */
    public static final String STATE_STYLE_ANALYSIS = "style_analysis";

    // ==================== 状态键 - 改写相关 ====================

    /**
     * 优化后的简历内容（JSON格式）
     */
    public static final String STATE_OPTIMIZED_SECTIONS = "optimized_sections";

    /**
     * 变更列表
     */
    public static final String STATE_CHANGES = "changes";

    /**
     * 改进分数
     */
    public static final String STATE_IMPROVEMENT_SCORE = "improvement_score";

    // ==================== 状态键 - ID映射 ====================

    /**
     * 简短标识符到真实ID的映射
     */
    public static final String STATE_SECTION_ID_MAP = "section_id_map";

    // ==================== 默认值 ====================

    /**
     * 默认空JSON对象
     */
    public static final String DEFAULT_EMPTY_JSON = "{}";

    /**
     * 默认目标岗位
     */
    public static final String DEFAULT_TARGET_POSITION = "未知岗位";

}
