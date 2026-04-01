package com.landit.interview.graph.review;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
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
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.landit.interview.graph.review.ReviewAnalysisGraphConstants.*;

/**
 * 收集面试数据节点
 * 收集面试基本信息、用户笔记、模拟面试转录等数据
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectInterviewDataNode implements NodeAction {

    private final InterviewCenterService interviewService;
    private final InterviewReviewNoteService reviewNoteService;
    private final JobPositionService jobPositionService;
    private final CompanyService companyService;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始收集面试数据 ===");
        String interviewId = (String) state.value(STATE_INTERVIEW_ID).orElse(null);
        if (interviewId == null) {
            log.warn("面试ID为空，无法收集数据");
            return Map.of(STATE_MESSAGES, List.of("面试ID为空"));
        }
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
        // TODO: 收集模拟面试转录（如有）
        String sessionTranscript = null;
        log.info("数据收集完成: 面试信息={}, 笔记={}", interviewData != null ? "有" : "无", reviewNoteData != null ? "有" : "无");
        List<String> messages = new ArrayList<>();
        messages.add("完成面试数据收集");
        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put(OUTPUT_NODE, NODE_COLLECT_DATA);
        nodeOutput.put(OUTPUT_PROGRESS, 30);
        nodeOutput.put(OUTPUT_MESSAGE, "数据收集完成");
        Map<String, Object> collectedData = new HashMap<>();
        collectedData.put("interview", interviewData);
        collectedData.put("reviewNote", reviewNoteData);
        collectedData.put("sessionTranscript", sessionTranscript);
        Map<String, Object> result = new HashMap<>();
        result.put(STATE_COLLECTED_DATA, JsonParseHelper.toJsonString(collectedData));
        result.put(STATE_INTERVIEW_DATA, interviewData);
        result.put(STATE_REVIEW_NOTES, reviewNoteData);
        result.put(STATE_SESSION_TRANSCRIPT, sessionTranscript);
        result.put(STATE_MESSAGES, messages);
        result.put(STATE_NODE_OUTPUT, nodeOutput);
        return result;
    }

}
