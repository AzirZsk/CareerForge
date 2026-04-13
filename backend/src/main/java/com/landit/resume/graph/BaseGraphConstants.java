package com.landit.resume.graph;

/**
 * Graph 工作流公共常量定义
 * 统一管理所有工作流共享的状态键、输出字段等字符串常量
 *
 * @author Azir
 */
public class BaseGraphConstants {

    protected BaseGraphConstants() {
        // 保护构造函数，允许子类继承但防止外部实例化
    }

    // ==================== 状态键 - 简历相关（共享） ====================

    /**
     * 简历内容（JSON格式）
     */
    public static final String STATE_RESUME_CONTENT = "resume_content";

    // ==================== 状态键 - 流程控制（共享） ====================

    /**
     * 当前步骤
     */
    public static final String STATE_CURRENT_STEP = "current_step";

    // ==================== 状态键 - 消息与输出（共享） ====================

    /**
     * 消息日志
     */
    public static final String STATE_MESSAGES = "messages";

    /**
     * 节点输出（用于SSE）
     */
    public static final String STATE_NODE_OUTPUT = "node_output";

    // ==================== 节点输出字段（共享） ====================

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

    /**
     * 节点输出 - 是否使用缓存
     */
    public static final String OUTPUT_CACHED = "cached";

}
