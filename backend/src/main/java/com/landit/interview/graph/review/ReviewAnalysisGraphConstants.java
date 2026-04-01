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
     * 收集面试数据节点
     */
    public static final String NODE_COLLECT_DATA = "collect_data";

    /**
     * AI分析表现节点
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

    // ==================== 状态键 - 中间结果 ====================

    /**
     * 收集的数据（JSON格式）
     */
    public static final String STATE_COLLECTED_DATA = "collected_data";

    /**
     * 面试基本信息
     */
    public static final String STATE_INTERVIEW_DATA = "interview_data";

    /**
     * 用户笔记
     */
    public static final String STATE_REVIEW_NOTES = "review_notes";

    /**
     * 模拟面试转录
     */
    public static final String STATE_SESSION_TRANSCRIPT = "session_transcript";

    /**
     * 分析结果（JSON格式）
     */
    public static final String STATE_ANALYSIS_RESULT = "analysis_result";

    /**
     * 改进建议列表
     */
    public static final String STATE_ADVICE_LIST = "advice_list";

}
