package com.careerforge.interview.graph.review;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.careerforge.common.config.prompt.ReviewPromptProperties;
import com.careerforge.common.config.prompt.PromptConfig;
import com.careerforge.common.util.ChatClientHelper;
import com.careerforge.common.util.JsonParseHelper;
import com.careerforge.interview.graph.review.dto.TranscriptAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.careerforge.interview.graph.review.ReviewAnalysisGraphConstants.*;

/**
 * 分析面试对话文本节点
 * 提取问答对，分析问题意图和回答清晰度
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzeTranscriptNode implements NodeAction {

    private final ChatClient chatClient;
    private final ReviewPromptProperties promptProperties;
    private final ReviewContextBuilder contextBuilder;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始分析面试对话 ===");
        // 获取面试ID并查询上下文
        String interviewId = (String) state.value(STATE_INTERVIEW_ID).orElse(null);
        ReviewContextBuilder.ReviewContext context = contextBuilder.buildContext(interviewId);
        String transcript = context.transcript() != null ? context.transcript() : "";
        // 空文本处理：跳过分析
        if (transcript.isBlank()) {
            log.warn("面试对话文本为空，跳过分析");
            return buildNodeResult(NODE_ANALYZE_TRANSCRIPT, 30, "对话文本为空，跳过分析",
                    "{}", STATE_TRANSCRIPT_ANALYSIS, "{}");
        }
        // 渲染提示词模板
        PromptConfig config = promptProperties.getAnalyzeTranscriptConfig();
        String systemPrompt = config.getSystemPrompt();
        Map<String, String> variables = Map.of(
                "companyName", context.companyName().isEmpty() ? "未知公司" : context.companyName(),
                "positionTitle", context.positionTitle().isEmpty() ? "未知职位" : context.positionTitle(),
                "jdContent", context.jdContent().isEmpty() ? "无JD信息" : context.jdContent(),
                "jdAnalysis", context.jdAnalysis().isEmpty() ? "无JD分析" : context.jdAnalysis(),
                "resumeContent", context.resumeContent().isEmpty() ? "无简历信息" : context.resumeContent(),
                "transcript", transcript
        );
        String userPrompt = ChatClientHelper.renderTemplate(config.getUserPromptTemplate(), variables);
        // 调用 AI 分析对话（使用结构化 DTO 接收响应）
        TranscriptAnalysisResult transcriptAnalysis = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, TranscriptAnalysisResult.class
        );
        log.info("面试对话分析完成");
        // 序列化为 JSON 存储（供后续节点使用）
        String transcriptAnalysisJson = JsonParseHelper.toJsonString(transcriptAnalysis);
        return buildNodeResult(NODE_ANALYZE_TRANSCRIPT, 30, "对话分析完成",
                transcriptAnalysis, STATE_TRANSCRIPT_ANALYSIS, transcriptAnalysisJson);
    }

}
