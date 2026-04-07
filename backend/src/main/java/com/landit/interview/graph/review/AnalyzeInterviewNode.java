package com.landit.interview.graph.review;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.util.ChatClientHelper;
import com.landit.common.util.JsonParseHelper;
import com.landit.company.entity.Company;
import com.landit.company.service.CompanyService;
import com.landit.interview.entity.Interview;
import com.landit.interview.entity.InterviewReviewNote;
import com.landit.interview.service.InterviewCenterService;
import com.landit.interview.service.InterviewReviewNoteService;
import com.landit.jobposition.entity.JobPosition;
import com.landit.jobposition.service.JobPositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.landit.interview.graph.review.ReviewAnalysisGraphConstants.*;

/**
 * AI分析面试表现节点
 * 先收集面试数据，再调用AI分析面试表现
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
    private final JobPositionService jobPositionService;
    private final CompanyService companyService;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String interviewId = (String) state.value(STATE_INTERVIEW_ID).orElse(null);
        String sessionTranscript = (String) state.value(STATE_SESSION_TRANSCRIPT).orElse(null);
        if (interviewId == null) {
            log.warn("面试ID为空，无法分析");
            return Map.of(STATE_MESSAGES, List.of("面试ID为空"));
        }
        // ==================== 第一步：收集数据 ====================
        log.info("=== 开始收集面试数据 ===");
        // 获取面试基本信息（包含轮次类型和名称）
        Interview interview = interviewService.getById(interviewId);
        Map<String, Object> interviewData = null;
        if (interview != null) {
            interviewData = new HashMap<>();
            interviewData.put("roundType", interview.getRoundType());
            interviewData.put("roundName", interview.getRoundName());
            interviewData.put("date", interview.getDate());
            interviewData.put("notes", interview.getNotes());
            // 通过 jobPositionId 关联查询公司名和职位名
            if (interview.getJobPositionId() != null) {
                JobPosition jobPosition = jobPositionService.getById(interview.getJobPositionId());
                if (jobPosition != null) {
                    interviewData.put("position", jobPosition.getTitle());
                    Company company = companyService.getById(jobPosition.getCompanyId());
                    interviewData.put("company", company != null ? company.getName() : "");
                }
            }
        }
        // 收集用户笔记
        InterviewReviewNote reviewNote = reviewNoteService.getManualNoteByInterviewId(interviewId);
        Map<String, Object> reviewNoteData = null;
        if (reviewNote != null) {
            reviewNoteData = new HashMap<>();
            reviewNoteData.put("overallFeeling", reviewNote.getOverallFeeling());
            reviewNoteData.put("highPoints", reviewNote.getHighPoints());
            reviewNoteData.put("weakPoints", reviewNote.getWeakPoints());
            reviewNoteData.put("lessonsLearned", reviewNote.getLessonsLearned());
            reviewNoteData.put("suggestions", reviewNote.getSuggestions());
        }
        // 获取前序节点的对话分析结果
        String transcriptAnalysis = (String) state.value(STATE_TRANSCRIPT_ANALYSIS).orElse(null);
        // 组装收集的数据
        Map<String, Object> collectedData = new HashMap<>();
        collectedData.put("interview", interviewData);
        collectedData.put("reviewNote", reviewNoteData);
        collectedData.put("sessionTranscript", sessionTranscript);
        // 如果有对话分析结果，也加入数据中
        if (transcriptAnalysis != null && !transcriptAnalysis.equals("{}")) {
            collectedData.put("transcriptAnalysis", transcriptAnalysis);
        }
        String collectedDataJson = JsonParseHelper.toJsonString(collectedData);
        log.info("数据收集完成: 面试信息={}, 笔记={}", interviewData != null ? "有" : "无", reviewNoteData != null ? "有" : "无");

        // ==================== 第二步：AI分析 ====================
        log.info("=== 开始AI分析面试表现 ===");
        AIPromptProperties.PromptConfig config = aiPromptProperties.getReviewGraph().getAnalyzeInterviewConfig();
        String systemPrompt = config.getSystemPrompt();
        String userPrompt = config.getUserPromptTemplate().replace("{collectedData}", collectedDataJson);
        String analysisResult = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, String.class
        );
        log.info("AI分析完成");

        List<String> messages = new ArrayList<>();
        messages.add("完成数据收集和AI分析");

        // 构建节点输出数据（用于 SSE）
        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put(OUTPUT_NODE, NODE_ANALYZE_INTERVIEW);
        nodeOutput.put(OUTPUT_PROGRESS, 70);
        nodeOutput.put(OUTPUT_MESSAGE, "AI分析完成");
        nodeOutput.put(OUTPUT_DATA, analysisResult);

        Map<String, Object> result = new HashMap<>();
        result.put(STATE_ANALYSIS_RESULT, analysisResult);
        result.put(STATE_MESSAGES, messages);
        result.put(STATE_NODE_OUTPUT, nodeOutput);
        return result;
    }

}
