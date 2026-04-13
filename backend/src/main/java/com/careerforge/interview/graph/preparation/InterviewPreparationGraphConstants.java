package com.careerforge.interview.graph.preparation;

import com.careerforge.resume.graph.BaseGraphConstants;

/**
 * 面试准备助手工作流常量定义
 *
 * @author Azir
 */
public final class InterviewPreparationGraphConstants extends BaseGraphConstants {

    private InterviewPreparationGraphConstants() {
    }

    // ==================== Graph 名称 ====================

    /**
     * 面试准备助手工作流 Graph 名称
     */
    public static final String GRAPH_INTERVIEW_PREPARATION = "interview_preparation";

    // ==================== 节点名称 ====================

    /**
     * 公司调研节点（内置检查逻辑）
     */
    public static final String NODE_COMPANY_RESEARCH = "company_research";

    /**
     * JD分析节点（内置检查逻辑）
     */
    public static final String NODE_JD_ANALYSIS = "jd_analysis";

    /**
     * 生成准备事项节点
     */
    public static final String NODE_GENERATE_PREPARATION = "generate_preparation";

    // ==================== 状态键 - 输入 ====================

    /**
     * 面试ID
     */
    public static final String STATE_INTERVIEW_ID = "interview_id";

    /**
     * 公司名称
     */
    public static final String STATE_COMPANY_NAME = "company_name";

    /**
     * 职位名称
     */
    public static final String STATE_POSITION_TITLE = "position_title";

    /**
     * JD原文
     */
    public static final String STATE_JD_CONTENT = "jd_content";

    /**
     * 简历内容（用于生成针对性的准备建议）
     */
    public static final String STATE_RESUME_CONTENT = "resume_content";

    /**
     * 上一轮面试的复盘笔记（JSON字符串，包含manual和ai_analysis两种）
     */
    public static final String STATE_PREVIOUS_REVIEW_NOTES = "previous_review_notes";

    // ==================== 状态键 - 中间结果 ====================

    /**
     * 公司ID
     */
    public static final String STATE_COMPANY_ID = "company_id";

    /**
     * 职位ID
     */
    public static final String STATE_JOB_POSITION_ID = "job_position_id";

    /**
     * 公司调研结果（JSON格式）
     */
    public static final String STATE_COMPANY_RESEARCH_RESULT = "company_research_result";

    /**
     * JD分析结果（JSON格式）
     */
    public static final String STATE_JD_ANALYSIS_RESULT = "jd_analysis_result";

    /**
     * 准备事项列表
     */
    public static final String STATE_PREPARATION_ITEMS = "preparation_items";

    /**
     * 节点输出（用于SSE）
     */
    public static final String STATE_NODE_OUTPUT = "node_output";

    // ==================== 默认值 ====================

    /**
     * 调研有效期（天）
     */
    public static final int RESEARCH_VALIDITY_DAYS = 30;

}
