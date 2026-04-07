package com.landit.interview.graph.review;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.util.ChatClientHelper;
import com.landit.common.util.JsonParseHelper;
import com.landit.interview.entity.Interview;
import com.landit.interview.entity.InterviewReviewNote;
import com.landit.interview.graph.review.dto.InterviewAnalysisResult;
import com.landit.interview.service.InterviewCenterService;
import com.landit.interview.service.InterviewReviewNoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.landit.interview.graph.review.ReviewAnalysisGraphConstants.*;

/**
 * AI分析面试表现节点
 * 基于对话分析结果 + 面试上下文（JD、简历、用户笔记），AI 综合分析面试表现
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzeInterviewNode implements NodeAction {

    private final ChatClient chatClient;
    private final AIPromptProperties aiPromptProperties;
    private final InterviewCenterService interviewService;
    private final InterviewReviewNoteService reviewNoteService;
    private final ReviewContextBuilder contextBuilder;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String interviewId = (String) state.value(STATE_INTERVIEW_ID).orElse(null);
        if (interviewId == null) {
            log.warn("面试ID为空，无法分析");
            return Map.of(STATE_MESSAGES, List.of("面试ID为空"));
        }
        // ==================== 第一步：收集数据 ====================
        log.info("=== 开始收集面试数据 ===");
        // 获取面试基本信息
        Interview interview = interviewService.getById(interviewId);
        Map<String, Object> interviewData = buildInterviewData(interview);
        // 收集用户笔记
        InterviewReviewNote reviewNote = reviewNoteService.getManualNoteByInterviewId(interviewId);
        Map<String, Object> reviewNoteData = buildReviewNoteData(reviewNote);
        // 通过 ContextBuilder 获取面试上下文
        ReviewContextBuilder.ReviewContext context = contextBuilder.buildContext(interviewId);
        String transcriptAnalysis = (String) state.value(STATE_TRANSCRIPT_ANALYSIS).orElse(null);
        // 组装收集的数据
        Map<String, Object> contextData = new LinkedHashMap<>();
        contextData.put("companyName", context.companyName());
        contextData.put("positionTitle", context.positionTitle());
        contextData.put("jdContent", context.jdContent());
        contextData.put("jdAnalysis", context.jdAnalysis());
        contextData.put("resumeContent", context.resumeContent());
        Map<String, Object> collectedData = new LinkedHashMap<>();
        collectedData.put("interview", interviewData);
        collectedData.put("reviewNote", reviewNoteData);
        collectedData.put("context", contextData);
        // 对话分析结果（从前序节点传来）
        if (transcriptAnalysis != null && !transcriptAnalysis.isBlank() && !"{}".equals(transcriptAnalysis)) {
            collectedData.put("transcriptAnalysis", transcriptAnalysis);
        }
        String collectedDataJson = JsonParseHelper.toJsonString(collectedData);
        log.info("数据收集完成: 面试信息={}, 笔记={}, 上下文(公司={}, 职位={}, JD={}, 简历={})",
                interviewData != null ? "有" : "无",
                reviewNoteData != null ? "有" : "无",
                context.companyName().isEmpty() ? "无" : "有",
                context.positionTitle().isEmpty() ? "无" : "有",
                context.jdContent().isEmpty() ? "无" : "有",
                context.resumeContent().isEmpty() ? "无" : "有");

        // ==================== 第二步：AI分析 ====================
        log.info("=== 开始AI分析面试表现 ===");
        AIPromptProperties.PromptConfig config = aiPromptProperties.getReviewGraph().getAnalyzeInterviewConfig();
        String systemPrompt = config.getSystemPrompt();
        String userPrompt = config.getUserPromptTemplate().replace("{collectedData}", collectedDataJson);
        // 使用结构化 DTO 接收响应
        InterviewAnalysisResult analysisResult = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, InterviewAnalysisResult.class
        );
        log.info("AI分析完成");
        // 序列化为 JSON 存储（供后续节点使用）
        String analysisResultJson = JsonParseHelper.toJsonString(analysisResult);
        return buildNodeResult(NODE_ANALYZE_INTERVIEW, 70, "AI分析完成",
                analysisResult, STATE_ANALYSIS_RESULT, analysisResultJson);
    }

    /**
     * 构建面试基本信息数据 Map
     */
    private Map<String, Object> buildInterviewData(Interview interview) {
        if (interview == null) {
            return null;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("roundType", interview.getRoundType());
        data.put("roundName", interview.getRoundName());
        data.put("date", interview.getDate());
        data.put("notes", interview.getNotes());
        data.put("interviewType", interview.getInterviewType());
        data.put("location", interview.getLocation());
        data.put("onlineLink", interview.getOnlineLink());
        return data;
    }

    /**
     * 构建复盘笔记数据 Map
     */
    private Map<String, Object> buildReviewNoteData(InterviewReviewNote reviewNote) {
        if (reviewNote == null) {
            return null;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("overallFeeling", reviewNote.getOverallFeeling());
        data.put("highPoints", reviewNote.getHighPoints());
        data.put("weakPoints", reviewNote.getWeakPoints());
        data.put("lessonsLearned", reviewNote.getLessonsLearned());
        data.put("suggestions", reviewNote.getSuggestions());
        return data;
    }

}
