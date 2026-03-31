package com.landit.interview.graph.review;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.landit.common.util.JsonParseHelper;
import com.landit.interview.dto.interviewcenter.RoundVO;
import com.landit.interview.dto.interviewcenter.ReviewNoteVO;
import com.landit.interview.entity.InterviewRound;
import com.landit.interview.entity.InterviewReviewNote;
import com.landit.interview.service.InterviewRoundService;
import com.landit.interview.service.InterviewReviewNoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.landit.interview.graph.review.ReviewAnalysisGraphConstants.*;

/**
 * 收集面试数据节点
 * 收集轮次记录、用户笔记、模拟面试转录等数据
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectInterviewDataNode implements NodeAction {

    private final InterviewRoundService roundService;
    private final InterviewReviewNoteService reviewNoteService;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始收集面试数据 ===");
        String interviewId = (String) state.value(STATE_INTERVIEW_ID).orElse(null);
        if (interviewId == null) {
            log.warn("面试ID为空，无法收集数据");
            return Map.of(STATE_MESSAGES, List.of("面试ID为空"));
        }
        // 收集轮次记录
        List<InterviewRound> rounds = roundService.getByInterviewId(interviewId);
        List<Map<String, Object>> roundsData = rounds.stream()
                .map(round -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", String.valueOf(round.getId()));
                    map.put("roundType", round.getRoundType());
                    map.put("roundName", round.getRoundName());
                    map.put("status", round.getStatus());
                    map.put("scheduledDate", round.getScheduledDate());
                    map.put("actualDate", round.getActualDate());
                    map.put("notes", round.getNotes());
                    map.put("selfRating", round.getSelfRating());
                    map.put("resultNote", round.getResultNote());
                    return map;
                })
                .collect(Collectors.toList());
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
        // 暂时为空，后续可扩展
        String sessionTranscript = null;
        log.info("数据收集完成: 轮次数={}, 笔记={}", roundsData.size(), reviewNoteData != null ? "有" : "无");
        List<String> messages = new ArrayList<>();
        messages.add("完成面试数据收集");
        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put(OUTPUT_NODE, NODE_COLLECT_DATA);
        nodeOutput.put(OUTPUT_PROGRESS, 30);
        nodeOutput.put(OUTPUT_MESSAGE, "数据收集完成");
        Map<String, Object> collectedData = new HashMap<>();
        collectedData.put("rounds", roundsData);
        collectedData.put("reviewNote", reviewNoteData);
        collectedData.put("sessionTranscript", sessionTranscript);
        Map<String, Object> result = new HashMap<>();
        result.put(STATE_COLLECTED_DATA, JsonParseHelper.toJsonString(collectedData));
        result.put(STATE_ROUNDS, roundsData);
        result.put(STATE_REVIEW_NOTES, reviewNoteData);
        result.put(STATE_SESSION_TRANSCRIPT, sessionTranscript);
        result.put(STATE_MESSAGES, messages);
        result.put(STATE_NODE_OUTPUT, nodeOutput);
        return result;
    }

}
