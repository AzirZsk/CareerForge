package com.landit.interview.graph.review;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.util.ChatClientHelper;
import com.landit.common.util.JsonParseHelper;
import com.landit.interview.graph.review.dto.AdviceItem;
import com.landit.interview.graph.review.dto.ListAdviceResponse;
import com.landit.interview.service.InterviewAIAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.landit.interview.graph.review.ReviewAnalysisGraphConstants.*;

/**
 * 生成改进建议节点
 * 根据分析结果生成具体的改进建议
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GenerateAdviceNode implements NodeAction {

    private final ChatClient chatClient;
    private final AIPromptProperties aiPromptProperties;
    private final InterviewAIAnalysisService aiAnalysisService;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始生成改进建议 ===");
        String analysisResult = (String) state.value(STATE_ANALYSIS_RESULT).orElse("{}");
        AIPromptProperties.PromptConfig config = aiPromptProperties.getReviewGraph().getGenerateAdviceConfig();
        String systemPrompt = config.getSystemPrompt();
        String userPrompt = config.getUserPromptTemplate().replace("{analysisResult}", analysisResult);
        // 使用结构化 DTO 接收响应（JSON Schema 约束）
        ListAdviceResponse response = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, ListAdviceResponse.class
        );
        log.info("改进建议生成完成");
        // 序列化为 JSON 存储
        String adviceJson = JsonParseHelper.toJsonString(response.getAdviceList());
        // 保存 AI 分析结果到数据库
        saveAnalysisResult(state, response.getAdviceList());
        return buildNodeResult(NODE_GENERATE_ADVICE, 100, "改进建议生成完成",
                response.getAdviceList(), STATE_ADVICE_LIST, adviceJson);
    }

    /**
     * 保存 AI 分析结果到数据库
     * 包含三个节点的完整数据：对话分析、面试分析、改进建议
     */
    private void saveAnalysisResult(OverAllState state, List<AdviceItem> adviceList) {
        // 获取面试ID
        String interviewId = (String) state.value(STATE_INTERVIEW_ID).orElse(null);
        if (interviewId == null) {
            log.warn("[GenerateAdviceNode] 面试ID为空，跳过保存");
            return;
        }
        // 从 state 中获取前序节点的分析结果
        String transcriptAnalysis = (String) state.value(STATE_TRANSCRIPT_ANALYSIS).orElse("{}");
        String interviewAnalysis = (String) state.value(STATE_ANALYSIS_RESULT).orElse("{}");
        try {
            aiAnalysisService.saveAnalysis(interviewId, adviceList, transcriptAnalysis, interviewAnalysis);
            log.info("[GenerateAdviceNode] AI 分析结果已保存: interviewId={}, adviceCount={}",
                    interviewId, adviceList.size());
        } catch (Exception e) {
            // 保存失败不中断工作流，只记录错误
            log.error("[GenerateAdviceNode] 保存 AI 分析结果失败: interviewId={}", interviewId, e);
        }
    }
}
