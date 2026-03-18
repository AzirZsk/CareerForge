package com.landit.resume.graph.tailor;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.util.ChatClientHelper;
import com.landit.common.util.JsonParseHelper;
import com.landit.resume.dto.JobRequirements;
import com.landit.resume.dto.MatchAnalysis;
import com.landit.resume.dto.ResumeDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.landit.resume.graph.tailor.TailorResumeGraphConstants.*;

/**
 * 匹配简历节点
 * 分析简历与 JD 的匹配程度
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class MatchResumeNode implements NodeAction {

    private final ChatClient chatClient;
    private final AIPromptProperties aiPromptProperties;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始匹配简历 ===");

        ResumeDetailVO resumeDetail = (ResumeDetailVO) state.value(STATE_RESUME_CONTENT).orElse(null);
        Objects.requireNonNull(resumeDetail, "简历内容不能为空");

        String jobRequirementsJson = (String) state.value(STATE_JOB_REQUIREMENTS).orElse("{}");
        JobRequirements jobRequirements = JsonParseHelper.parseToEntity(jobRequirementsJson, JobRequirements.class);

        String targetPosition = (String) state.value(STATE_TARGET_POSITION).orElse("");
        // 传入结构化区块数据而非 Markdown 纯文本，便于 AI 精确提取信息
        String resumeContent = JsonParseHelper.toJsonString(
                Map.of(
                        "targetPosition", targetPosition,
                        "sections", resumeDetail.getSections() != null ? resumeDetail.getSections() : List.of()
                )
        );

        // 使用拆分提示词调用
        AIPromptProperties.PromptConfig promptConfig = aiPromptProperties.getTailorGraph().getMatchResumeConfig();
        String systemPrompt = promptConfig.getSystemPrompt();
        String userPrompt = ChatClientHelper.renderTemplate(
                promptConfig.getUserPromptTemplate(),
                Map.of(
                        "targetPosition", targetPosition,
                        "resumeContent", resumeContent,
                        "jobRequirements", jobRequirementsJson
                )
        );

        // 调用 AI 并自动解析
        MatchAnalysis response = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, MatchAnalysis.class
        );

        String matchAnalysis = JsonParseHelper.toJsonString(response);

        log.info("简历匹配完成，匹配分数: {}", response.getMatchScore());

        List<String> messages = new ArrayList<>();
        messages.add("完成简历匹配分析");

        // 构建节点输出数据（用于 SSE）
        Map<String, Object> nodeOutput = JsonParseHelper.buildNodeOutput(
                NODE_MATCH_RESUME,
                60,
                "简历匹配分析完成",
                response
        );

        return Map.of(
                STATE_MATCH_ANALYSIS, matchAnalysis,
                STATE_MATCH_SCORE, response.getMatchScore(),
                STATE_MESSAGES, messages,
                STATE_CURRENT_STEP, NODE_MATCH_RESUME,
                STATE_NODE_OUTPUT, nodeOutput
        );
    }
}
