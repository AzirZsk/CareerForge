package com.landit.interview.graph.preparation;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.landit.common.config.AIPromptProperties;
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
 * 检查职位是否存在且分析是否有效，如有效则使用缓存，否则执行AI分析
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JDAnalysisNode implements NodeAction {

    private final ChatClient chatClient;
    private final AIPromptProperties aiPromptProperties;
    private final JobPositionService jobPositionService;
    private final PreparationContextBuilder contextBuilder;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始JD分析 ===");
        // 从工作流状态中获取公司ID（前序节点产出）
        String companyId = (String) state.value(STATE_COMPANY_ID).orElse(null);
        // 通过 ContextBuilder 获取职位名称和JD内容
        String interviewId = (String) state.value(STATE_INTERVIEW_ID).orElse(null);
        PreparationContextBuilder.PreparationContext context = contextBuilder.buildContext(interviewId);
        String positionTitle = context.positionTitle();
        String jdContent = context.jdContent();

        // 如果没有公司ID或职位名称，跳过分析
        if (companyId == null || companyId.isEmpty() || positionTitle == null || positionTitle.isEmpty()) {
            log.info("缺少公司ID或职位名称，跳过JD分析");
            return buildSkippedResult();
        }

        // 检查职位是否存在且分析是否有效
        JobPosition existingPosition = jobPositionService.findByCompanyIdAndTitle(companyId, positionTitle).orElse(null);
        if (existingPosition != null && jobPositionService.hasValidAnalysis(existingPosition)) {
            log.info("JD分析已存在且有效，使用缓存: jobPositionId={}", existingPosition.getId());
            return buildCachedResult(existingPosition);
        }

        // 需要执行AI分析
        log.info("执行AI JD分析: {}", positionTitle);
        return executeAIAnalysis(companyId, positionTitle, jdContent, existingPosition);
    }

    /**
     * 执行AI分析并持久化结果
     */
    private Map<String, Object> executeAIAnalysis(String companyId, String positionTitle, String jdContent, JobPosition existingPosition) {
        // 获取AI提示词配置
        AIPromptProperties.PromptConfig config = aiPromptProperties.getPreparationGraph().getJdAnalysisConfig();
        String systemPrompt = config.getSystemPrompt();
        String userPrompt = config.getUserPromptTemplate()
                .replace("{positionTitle}", positionTitle)
                .replace("{jdContent}", jdContent != null ? jdContent : "无");

        // 调用AI生成JD分析
        JDAnalysisResult analysisResult = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, JDAnalysisResult.class
        );

        // 持久化分析结果到数据库
        String analysisJson = JsonParseHelper.toJsonString(analysisResult);
        JobPosition jobPosition = jobPositionService.createOrUpdateJobPosition(
                companyId, positionTitle, jdContent, analysisJson
        );
        String jobPositionId = String.valueOf(jobPosition.getId());

        log.info("AI JD分析完成: jobPositionId={}", jobPositionId);

        // 构建节点输出
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

    /**
     * 构建缓存结果（分析已存在且有效时使用）
     */
    private Map<String, Object> buildCachedResult(JobPosition jobPosition) {
        String jobPositionId = String.valueOf(jobPosition.getId());
        String analysisJson = jobPosition.getJdAnalysis();

        List<String> messages = new ArrayList<>();
        messages.add("JD分析已存在（使用缓存）");

        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put(OUTPUT_NODE, NODE_JD_ANALYSIS);
        nodeOutput.put(OUTPUT_PROGRESS, 60);
        nodeOutput.put(OUTPUT_MESSAGE, "JD分析完成（使用缓存）");
        nodeOutput.put(OUTPUT_CACHED, true);  // 标识使用缓存

        // 缓存数据也解析出来用于前端展示
        JDAnalysisResult analysisResult = JsonParseHelper.parseToEntity(analysisJson, JDAnalysisResult.class);
        if (analysisResult != null) {
            nodeOutput.put(OUTPUT_DATA, Map.of("jdAnalysis", analysisResult));
        }

        Map<String, Object> result = new HashMap<>();
        result.put(STATE_JOB_POSITION_ID, jobPositionId);
        result.put(STATE_JD_ANALYSIS_RESULT, analysisJson != null ? analysisJson : "{}");
        result.put(STATE_MESSAGES, messages);
        result.put(STATE_NODE_OUTPUT, nodeOutput);
        return result;
    }

    /**
     * 构建跳过结果（缺少必要信息时使用）
     */
    private Map<String, Object> buildSkippedResult() {
        List<String> messages = new ArrayList<>();
        messages.add("跳过JD分析（缺少公司ID或职位名称）");

        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put(OUTPUT_NODE, NODE_JD_ANALYSIS);
        nodeOutput.put(OUTPUT_PROGRESS, 60);
        nodeOutput.put(OUTPUT_MESSAGE, "跳过JD分析");

        Map<String, Object> result = new HashMap<>();
        result.put(STATE_JOB_POSITION_ID, null);
        result.put(STATE_JD_ANALYSIS_RESULT, "{}");
        result.put(STATE_MESSAGES, messages);
        result.put(STATE_NODE_OUTPUT, nodeOutput);
        return result;
    }

}
