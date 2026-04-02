package com.landit.interview.graph.preparation;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.util.ChatClientHelper;
import com.landit.common.util.JsonParseHelper;
import com.landit.jobposition.dto.JDAnalysisResult;
import com.landit.jobposition.entity.JobPosition;
import com.landit.jobposition.handler.JobPositionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.landit.interview.graph.preparation.InterviewPreparationGraphConstants.*;

/**
 * JD分析节点
 * 使用AI分析职位描述
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JDAnalysisNode implements NodeAction {

    private final ChatClient chatClient;
    private final AIPromptProperties aiPromptProperties;
    private final JobPositionHandler jobPositionHandler;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始JD分析 ===");
        String companyId = (String) state.value(STATE_COMPANY_ID).orElse(null);
        String positionTitle = (String) state.value(STATE_POSITION_TITLE).orElse(null);
        String jdContent = (String) state.value(STATE_JD_CONTENT).orElse(null);
        String jobPositionId = (String) state.value(STATE_JOB_POSITION_ID).orElse(null);
        AIPromptProperties.PromptConfig config = aiPromptProperties.getPreparationGraph().getJdAnalysisConfig();
        String systemPrompt = config.getSystemPrompt();
        String userPrompt = config.getUserPromptTemplate()
                .replace("{positionTitle}", positionTitle)
                .replace("{jdContent}", jdContent != null ? jdContent : "无");
        JDAnalysisResult analysisResult = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, JDAnalysisResult.class
        );
        String analysisJson = JsonParseHelper.toJsonString(analysisResult);
        JobPosition jobPosition = jobPositionHandler.saveAnalysis(
                companyId, positionTitle, jdContent, analysisJson
        );
        if (jobPositionId == null) {
            jobPositionId = String.valueOf(jobPosition.getId());
        }
        log.info("JD分析完成: jobPositionId={}", jobPositionId);
        List<String> messages = new ArrayList<>();
        messages.add("完成JD分析: " + positionTitle);
        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put(OUTPUT_NODE, NODE_JD_ANALYSIS);
        nodeOutput.put(OUTPUT_PROGRESS, 60);
        nodeOutput.put(OUTPUT_MESSAGE, "JD分析完成");
        nodeOutput.put(OUTPUT_DATA, Map.of("jdAnalysis", analysisResult));
        Map<String, Object> result = new HashMap<>();
        result.put(STATE_JOB_POSITION_ID, jobPositionId);
        result.put(STATE_JD_ANALYSIS_RESULT, analysisJson);
        result.put(STATE_MESSAGES, messages);
        result.put(STATE_NODE_OUTPUT, nodeOutput);
        return result;
    }

}
