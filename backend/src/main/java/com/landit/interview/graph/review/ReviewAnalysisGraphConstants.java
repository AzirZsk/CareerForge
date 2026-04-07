package com.landit.interview.graph.review;

import com.landit.resume.graph.BaseGraphConstants;

/**
 * 复盘AI分析工作流常量定义
 *
 * @author Azir
 */
public final class ReviewAnalysisGraphConstants extends BaseGraphConstants {

    private ReviewAnalysisGraphConstants() {
    }

    // ==================== Graph 名称 ====================

    /**
     * 复盘AI分析工作流 Graph 名称
     */
    public static final String GRAPH_REVIEW_ANALYSIS = "review_analysis";

    // ==================== 节点名称 ====================

    /**
     * AI分析表现节点（合并数据收集+AI分析）
     */
    public static final String NODE_ANALYZE_INTERVIEW = "analyze_interview";

    /**
     * 生成改进建议节点
     */
    public static final String NODE_GENERATE_ADVICE = "generate_advice";

    // ==================== 状态键 - 输入 ====================

    /**
     * 面试ID
     */
    public static final String STATE_INTERVIEW_ID = "interview_id";

    /**
     * 面试过程转录文本（用户输入）
     */
    public static final String STATE_SESSION_TRANSCRIPT = "session_transcript";

    // ==================== 节点输出字段 ====================

    /**
     * 节点输出 - 节点名称
     */
    public static final String OUTPUT_NODE = "node";

    /**
     * 节点输出 - 进度
     */
    public static final String OUTPUT_PROGRESS = "progress";

    // ==================== 状态键 - 中间结果 ====================

    /**
     * 分析结果（JSON格式）
     */
    public static final String STATE_ANALYSIS_RESULT = "analysis_result";

    /**
     * 改进建议列表
     */
    public static final String STATE_ADVICE_LIST = "advice_list";

}
