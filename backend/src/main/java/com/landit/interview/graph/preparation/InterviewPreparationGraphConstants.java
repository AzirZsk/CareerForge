package com.landit.interview.graph.preparation;

import com.landit.resume.graph.BaseGraphConstants;

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
     * 检查公司节点
     */
    public static final String NODE_CHECK_COMPANY = "check_company";

    /**
     * 公司调研节点
     */
    public static final String NODE_COMPANY_RESEARCH = "company_research";

    /**
     * 检查职位节点
     */
    public static final String NODE_CHECK_JOB_POSITION = "check_job_position";

    /**
     * JD分析节点
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

    // ==================== 状态键 - 中间结果 ====================

    /**
     * 公司ID
     */
    public static final String STATE_COMPANY_ID = "company_id";

    /**
     * 需要公司调研
     */
    public static final String STATE_NEED_COMPANY_RESEARCH = "need_company_research";

    /**
     * 职位ID
     */
    public static final String STATE_JOB_POSITION_ID = "job_position_id";

    /**
     * 需要JD分析
     */
    public static final String STATE_NEED_JD_ANALYSIS = "need_jd_analysis";

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

    // ==================== 路由决策 ====================

    /**
     * 需要执行公司调研
     */
    public static final String ROUTE_NEED_RESEARCH = "need_research";

    /**
     * 跳过公司调研
     */
    public static final String ROUTE_SKIP_RESEARCH = "skip_research";

    /**
     * 需要执行JD分析
     */
    public static final String ROUTE_NEED_ANALYSIS = "need_analysis";

    /**
     * 跳过JD分析
     */
    public static final String ROUTE_SKIP_ANALYSIS = "skip_analysis";

    // ==================== 默认值 ====================

    /**
     * 调研有效期（天）
     */
    public static final int RESEARCH_VALIDITY_DAYS = 30;

}
