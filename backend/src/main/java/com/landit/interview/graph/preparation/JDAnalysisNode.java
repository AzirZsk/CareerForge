package com.landit.interview.graph.preparation;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.landit.common.util.ChatClientHelper;
import com.landit.common.util.JsonParseHelper;
import com.landit.jobposition.dto.JDAnalysisResult;
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
    private final JobPositionService jobPositionService;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始JD分析 ===");
        String companyId = (String) state.value(STATE_COMPANY_ID).orElse(null);
        String positionTitle = (String) state.value(STATE_POSITION_TITLE).orElse(null);
        String jdContent = (String) state.value(STATE_JD_CONTENT).orElse(null);
        String jobPositionId = (String) state.value(STATE_JOB_POSITION_ID).orElse(null);
        String systemPrompt = """
                你是一位专业的职位分析专家。请根据职位描述，提供以下分析：
                1. 职位概述（职责范围、核心目标）
                2. 必备技能（必须掌握的技术和技能）
                3. 加分技能（优先考虑的技能）
                4. 关键关键词（简历和面试中应出现的关键词）
                5. 职责重点（主要工作内容）
                6. 任职要求（学历、经验、软技能等）
                7. 面试重点（可能的面试问题方向）
                8. 准备建议（针对性的准备建议）

                请以JSON格式返回结果。
                """;
        String userPrompt = "职位名称：" + positionTitle + "\n\n职位描述：\n" + (jdContent != null ? jdContent : "无");
        JDAnalysisResult analysisResult = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, JDAnalysisResult.class
        );
        String analysisJson = JsonParseHelper.toJsonString(analysisResult);
        JobPosition jobPosition = jobPositionService.createOrUpdateJobPosition(
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
        nodeOutput.put(OUTPUT_DATA, analysisResult);
        Map<String, Object> result = new HashMap<>();
        result.put(STATE_JOB_POSITION_ID, jobPositionId);
        result.put(STATE_JD_ANALYSIS_RESULT, analysisJson);
        result.put(STATE_MESSAGES, messages);
        result.put(STATE_NODE_OUTPUT, nodeOutput);
        return result;
    }

}
