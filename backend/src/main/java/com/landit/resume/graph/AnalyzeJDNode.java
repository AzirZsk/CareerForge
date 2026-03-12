package com.landit.resume.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.util.ChatClientHelper;
import com.landit.common.util.JsonParseHelper;
import com.landit.resume.dto.JobRequirements;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.landit.resume.graph.TailorResumeGraphConstants.*;

/**
 * 分析 JD 节点
 * 从职位描述中提取关键信息
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class AnalyzeJDNode implements NodeAction {

    private final ChatClient chatClient;
    private final AIPromptProperties aiPromptProperties;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始分析 JD ===");

        String targetPosition = (String) state.value(STATE_TARGET_POSITION).orElse("");
        String jobDescription = (String) state.value(STATE_JOB_DESCRIPTION).orElse("");

        // 使用拆分提示词调用
        AIPromptProperties.PromptConfig promptConfig = aiPromptProperties.getTailorGraph().getAnalyzeJDConfig();
        String systemPrompt = promptConfig.getSystemPrompt();
        String userPrompt = ChatClientHelper.renderTemplate(
                promptConfig.getUserPromptTemplate(),
                Map.of("targetPosition", targetPosition, "jobDescription", jobDescription)
        );

        // 调用 AI 并自动解析
        JobRequirements response = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, JobRequirements.class
        );

        String jobRequirements = JsonParseHelper.toJsonString(response);

        log.info("JD 分析完成，提取到 {} 个必备技能", response.getRequiredSkills() != null ? response.getRequiredSkills().size() : 0);

        List<String> messages = new ArrayList<>();
        messages.add("完成 JD 分析");

        // 构建节点输出数据（用于 SSE）
        Map<String, Object> nodeOutput = JsonParseHelper.buildNodeOutput(
                NODE_ANALYZE_JD,
                35,
                "职位描述分析完成",
                response
        );

        return Map.of(
                STATE_JOB_REQUIREMENTS, jobRequirements,
                STATE_MESSAGES, messages,
                STATE_CURRENT_STEP, NODE_ANALYZE_JD,
                STATE_NODE_OUTPUT, nodeOutput
        );
    }
}
