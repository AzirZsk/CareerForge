package com.landit.interview.graph.review;

import com.landit.resume.graph.BaseGraphConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * 分析面试对话节点（提取问答对、分析问题意图和回答清晰度）
     */
    public static final String NODE_ANALYZE_TRANSCRIPT = "analyze_transcript";

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

    // ==================== 状态键 - 面试上下文（Handler 注入） ====================

    /**
     * 公司名称
     */
    public static final String STATE_COMPANY_NAME = "company_name";

    /**
     * 职位名称
     */
    public static final String STATE_POSITION_TITLE = "position_title";

    /**
     * JD 原文（来自 Interview 或 JobPosition）
     */
    public static final String STATE_JD_CONTENT = "jd_content";

    /**
     * JD 分析结果 JSON（来自 JobPosition.jdAnalysis）
     */
    public static final String STATE_JD_ANALYSIS = "jd_analysis";

    /**
     * 简历摘要文本（来自 Handler.buildResumeContext）
     */
    public static final String STATE_RESUME_CONTENT = "resume_content";

    // ==================== 状态键 - 中间结果 ====================

    /**
     * 对话分析结果（包含问答对分析）
     */
    public static final String STATE_TRANSCRIPT_ANALYSIS = "transcript_analysis";

    /**
     * 分析结果（JSON格式）
     */
    public static final String STATE_ANALYSIS_RESULT = "analysis_result";

    /**
     * 改进建议列表
     */
    public static final String STATE_ADVICE_LIST = "advice_list";

    // ==================== 节点输出构建工具方法 ====================

    /**
     * 构建节点的标准返回结果
     * 统一 nodeOutput + result Map 的构建逻辑
     *
     * @param nodeName     节点名称
     * @param progress     进度百分比
     * @param message      进度消息
     * @param data         节点输出数据
     * @param stateKey     状态存储键（如 STATE_TRANSCRIPT_ANALYSIS）
     * @param stateValue   状态存储值
     * @return 标准节点返回结果
     */
    public static Map<String, Object> buildNodeResult(
            String nodeName, int progress, String message,
            Object data, String stateKey, Object stateValue) {
        // 构建节点输出（用于 SSE 推送）
        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put(OUTPUT_NODE, nodeName);
        nodeOutput.put(OUTPUT_PROGRESS, progress);
        nodeOutput.put(OUTPUT_MESSAGE, message);
        nodeOutput.put(OUTPUT_DATA, data);
        // 构建状态更新
        Map<String, Object> result = new HashMap<>();
        result.put(stateKey, stateValue);
        result.put(STATE_MESSAGES, List.of(message));
        result.put(STATE_NODE_OUTPUT, nodeOutput);
        return result;
    }

}
