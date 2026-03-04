package com.landit.resume.graph;

/**
 * 简历优化工作流常量定义
 * 统一管理 Graph 相关的节点名称、状态键、模式等字符串常量
 *
 * @author Azir
 */
public final class ResumeOptimizeGraphConstants {

    private ResumeOptimizeGraphConstants() {
        // 私有构造函数，防止实例化
    }

    // ==================== Graph 名称 ====================

    /**
     * 简历优化工作流 Graph 名称
     */
    public static final String GRAPH_RESUME_OPTIMIZE = "resume_optimize";

    // ==================== 节点名称 ====================

    /**
     * 快速诊断节点
     */
    public static final String NODE_DIAGNOSE_QUICK = "diagnose_quick";

    /**
     * 生成优化建议节点
     */
    public static final String NODE_GENERATE_SUGGESTIONS = "generate_suggestions";

    /**
     * 模块内容优化节点
     */
    public static final String NODE_OPTIMIZE_SECTION = "optimize_section";

    // ==================== 状态键 - 简历相关 ====================

    /**
     * 简历内容（JSON格式）
     */
    public static final String STATE_RESUME_CONTENT = "resume_content";

    // ==================== 状态键 - 诊断相关 ====================

    /**
     * 诊断模式（quick/precise）
     */
    public static final String STATE_DIAGNOSIS_MODE = "diagnosis_mode";

    /**
     * 诊断结果（JSON格式）
     */
    public static final String STATE_DIAGNOSIS_RESULT = "diagnosis_result";

    /**
     * 总体评分
     */
    public static final String STATE_OVERALL_SCORE = "overall_score";

    /**
     * 维度评分
     */
    public static final String STATE_DIMENSIONS = "dimensions";

    /**
     * 问题列表
     */
    public static final String STATE_ISSUES = "issues";

    /**
     * 亮点列表
     */
    public static final String STATE_HIGHLIGHTS = "highlights";

    /**
     * 快速改进建议
     */
    public static final String STATE_QUICK_WINS = "quick_wins";

    // ==================== 状态键 - 优化相关 ====================

    /**
     * 优化建议（JSON格式）
     */
    public static final String STATE_SUGGESTIONS = "suggestions";

    /**
     * 用户选择的建议
     */
    public static final String STATE_SELECTED_SUGGESTIONS = "selected_suggestions";

    /**
     * 建议列表
     */
    public static final String STATE_SUGGESTION_LIST = "suggestion_list";

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

    // ==================== 状态键 - 流程控制 ====================

    /**
     * 当前步骤
     */
    public static final String STATE_CURRENT_STEP = "current_step";

    /**
     * 下一个节点
     */
    public static final String STATE_NEXT_NODE = "next_node";

    // ==================== 状态键 - 消息与输出 ====================

    /**
     * 消息日志
     */
    public static final String STATE_MESSAGES = "messages";

    /**
     * 节点输出（用于SSE）
     */
    public static final String STATE_NODE_OUTPUT = "node_output";

    /**
     * 最后执行的节点
     */
    public static final String STATE_LAST_NODE = "last_node";

    // ==================== 状态键 - 版本相关 ====================

    /**
     * 版本ID
     */
    public static final String STATE_VERSION_ID = "version_id";

    /**
     * 版本名称
     */
    public static final String STATE_VERSION_NAME = "version_name";

    /**
     * 线程ID
     */
    public static final String STATE_THREAD_ID = "thread_id";

    // ==================== 诊断模式 ====================

    /**
     * 快速模式
     */
    public static final String MODE_QUICK = "quick";

    // ==================== 默认值 ====================

    /**
     * 默认目标岗位
     */
    public static final String DEFAULT_TARGET_POSITION = "未知岗位";

    /**
     * 默认空JSON对象
     */
    public static final String DEFAULT_EMPTY_JSON = "{}";

    /**
     * 默认空JSON数组
     */
    public static final String DEFAULT_EMPTY_ARRAY = "[]";

    // ==================== 节点输出字段 ====================

    /**
     * 节点输出 - 节点名称
     */
    public static final String OUTPUT_NODE = "node";

    /**
     * 节点输出 - 进度
     */
    public static final String OUTPUT_PROGRESS = "progress";

    /**
     * 节点输出 - 消息
     */
    public static final String OUTPUT_MESSAGE = "message";

    /**
     * 节点输出 - 数据
     */
    public static final String OUTPUT_DATA = "data";

}
