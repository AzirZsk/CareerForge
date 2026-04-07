package com.landit.interview.graph.review;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.util.ChatClientHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.landit.interview.graph.review.ReviewAnalysisGraphConstants.*;

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
    private final AIPromptProperties aiPromptProperties;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始分析面试对话 ===");
        // 获取输入
        String sessionTranscript = (String) state.value(STATE_SESSION_TRANSCRIPT).orElse("");
        // 空文本处理：跳过分析
        if (sessionTranscript == null || sessionTranscript.isBlank()) {
            log.warn("面试对话文本为空，跳过分析");
            return buildNodeResult(NODE_ANALYZE_TRANSCRIPT, 30, "对话文本为空，跳过分析",
                    "{}", STATE_TRANSCRIPT_ANALYSIS, "{}");
        }
        // 读取面试上下文
        String companyName = (String) state.value(STATE_COMPANY_NAME).orElse("");
        String positionTitle = (String) state.value(STATE_POSITION_TITLE).orElse("");
        String jdContent = (String) state.value(STATE_JD_CONTENT).orElse("");
        String jdAnalysis = (String) state.value(STATE_JD_ANALYSIS).orElse("");
        String resumeContent = (String) state.value(STATE_RESUME_CONTENT).orElse("");
        // 渲染提示词模板
        AIPromptProperties.PromptConfig config = aiPromptProperties.getReviewGraph().getAnalyzeTranscriptConfig();
        String systemPrompt = config.getSystemPrompt();
        Map<String, String> variables = Map.of(
                "companyName", companyName.isEmpty() ? "未知公司" : companyName,
                "positionTitle", positionTitle.isEmpty() ? "未知职位" : positionTitle,
                "jdContent", jdContent.isEmpty() ? "无JD信息" : jdContent,
                "jdAnalysis", jdAnalysis.isEmpty() ? "无JD分析" : jdAnalysis,
                "resumeContent", resumeContent.isEmpty() ? "无简历信息" : resumeContent,
                "sessionTranscript", sessionTranscript
        );
        String userPrompt = ChatClientHelper.renderTemplate(config.getUserPromptTemplate(), variables);
        // 调用 AI 分析对话
        String transcriptAnalysis = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, String.class
        );
        log.info("面试对话分析完成");
        return buildNodeResult(NODE_ANALYZE_TRANSCRIPT, 30, "对话分析完成",
                transcriptAnalysis, STATE_TRANSCRIPT_ANALYSIS, transcriptAnalysis);
    }

}
