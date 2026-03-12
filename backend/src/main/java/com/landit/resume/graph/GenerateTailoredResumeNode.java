package com.landit.resume.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.util.ChatClientHelper;
import com.landit.common.util.JsonParseHelper;
import com.landit.resume.dto.JobRequirements;
import com.landit.resume.dto.MatchAnalysis;
import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.dto.TailorResumeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.landit.resume.graph.TailorResumeGraphConstants.*;

/**
 * 生成定制简历节点
 * 根据 JD 和匹配分析生成定制简历
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class GenerateTailoredResumeNode implements NodeAction {

    private final ChatClient chatClient;
    private final AIPromptProperties aiPromptProperties;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始生成定制简历 ===");

        ResumeDetailVO resumeDetail = (ResumeDetailVO) state.value(STATE_RESUME_CONTENT).orElse(null);
        Objects.requireNonNull(resumeDetail, "简历内容不能为空");

        String jobRequirementsJson = (String) state.value(STATE_JOB_REQUIREMENTS).orElse("{}");
        String matchAnalysisJson = (String) state.value(STATE_MATCH_ANALYSIS).orElse("{}");
        String targetPosition = (String) state.value(STATE_TARGET_POSITION).orElse("");

        // 获取简历结构化内容
        String resumeContent = JsonParseHelper.toJsonString(resumeDetail);

        // 使用拆分提示词调用
        AIPromptProperties.PromptConfig promptConfig = aiPromptProperties.getTailorGraph().getGenerateTailoredConfig();
        String systemPrompt = promptConfig.getSystemPrompt();
        String userPrompt = ChatClientHelper.renderTemplate(
                promptConfig.getUserPromptTemplate(),
                Map.of(
                        "targetPosition", targetPosition,
                        "resumeContent", resumeContent,
                        "jobRequirements", jobRequirementsJson,
                        "matchAnalysis", matchAnalysisJson
                )
        );

        // 调用 AI 并自动解析
        TailorResumeResponse response = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, TailorResumeResponse.class
        );

        String tailoredResume = JsonParseHelper.toJsonString(response);

        log.info("定制简历生成完成，调整说明: {} 条", response.getTailorNotes() != null ? response.getTailorNotes().size() : 0);

        List<String> messages = new ArrayList<>();
        messages.add("完成定制简历生成");

        // 构建节点输出数据（用于 SSE）
        Map<String, Object> nodeOutput = JsonParseHelper.buildNodeOutput(
                NODE_GENERATE_TAILORED,
                100,
                "定制简历生成完成",
                response
        );

        return Map.of(
                STATE_TAILORED_RESUME, tailoredResume,
                STATE_TAILORED_SECTIONS, tailoredResume,
                STATE_MESSAGES, messages,
                STATE_CURRENT_STEP, NODE_GENERATE_TAILORED,
                STATE_NODE_OUTPUT, nodeOutput
        );
    }
}
