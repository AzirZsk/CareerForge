package com.landit.resume.graph.tailor;

import com.landit.resume.graph.BaseGraphConstants;

/**
 * 职位适配工作流常量定义
 * 统一管理 Graph 相关的节点名称、状态键等字符串常量
 *
 * @author Azir
 */
public final class TailorResumeGraphConstants extends BaseGraphConstants {

    private TailorResumeGraphConstants() {
        // 私有构造函数，防止实例化
    }

    // ==================== Graph 名称 ====================

    /**
     * 职位适配工作流 Graph 名称
     */
    public static final String GRAPH_TAILOR_RESUME = "tailor_resume";

    // ==================== 节点名称 ====================

    /**
     * 分析 JD 节点
     */
    public static final String NODE_ANALYZE_JD = "analyze_jd";

    /**
     * 匹配简历节点
     */
    public static final String NODE_MATCH_RESUME = "match_resume";

    /**
     * 生成定制简历节点
     */
    public static final String NODE_GENERATE_TAILORED = "generate_tailored";

    // ==================== 状态键 - 输入相关（特有） ====================

    /**
     * 目标职位
     */
    public static final String STATE_TARGET_POSITION = "target_position";

    /**
     * 职位描述（Job Description）
     */
    public static final String STATE_JOB_DESCRIPTION = "job_description";

    // ==================== 状态键 - 分析结果（特有） ====================

    /**
     * JD 分析结果（JSON格式）
     */
    public static final String STATE_JOB_REQUIREMENTS = "job_requirements";

    // ==================== 状态键 - 匹配结果（特有） ====================

    /**
     * 匹配分析结果（JSON格式）
     */
    public static final String STATE_MATCH_ANALYSIS = "match_analysis";

    /**
     * 匹配分数
     */
    public static final String STATE_MATCH_SCORE = "match_score";

    // ==================== 状态键 - 输出相关（特有） ====================

    /**
     * 定制简历结果（JSON格式）
     */
    public static final String STATE_TAILORED_RESUME = "tailored_resume";

    /**
     * 定制后的模块内容
     */
    public static final String STATE_TAILORED_SECTIONS = "tailored_sections";

    // ==================== 状态键 - 对比编辑相关 ====================

    /**
     * 优化前的区块数据（用于对比视图）
     */
    public static final String STATE_BEFORE_SECTION = "before_section";

    /**
     * 优化后的区块数据（用于对比视图）
     */
    public static final String STATE_AFTER_SECTION = "after_section";

    /**
     * 提升分数（对比视图展示）
     */
    public static final String STATE_IMPROVEMENT_SCORE = "improvement_score";

}
