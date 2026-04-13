package com.careerforge.interview.graph.preparation;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.careerforge.common.config.AIPromptProperties;
import com.careerforge.common.util.ChatClientHelper;
import com.careerforge.interview.dto.GeneratePreparationResult;
import com.careerforge.interview.dto.PreparationItemResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.careerforge.interview.graph.preparation.InterviewPreparationGraphConstants.*;

/**
 * 生成准备事项节点
 * 根据公司调研和JD分析结果，生成面试准备事项（仅返回数据，不保存到数据库）
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeneratePreparationNode implements NodeAction {

    private final ChatClient chatClient;
    private final AIPromptProperties aiPromptProperties;
    private final PreparationContextBuilder preparationContextBuilder;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始生成准备事项 ===");
        // 从工作流状态中提取上下文信息
        String interviewId = (String) state.value(STATE_INTERVIEW_ID).orElse(null);
        if (interviewId == null) {
            log.warn("面试ID为空，跳过准备事项生成");
            return buildEmptyResult();
        }
        // 通过 ContextBuilder 获取基本信息
        PreparationContextBuilder.PreparationContext context = preparationContextBuilder.buildContext(interviewId);
        String companyName = context.companyName();
        String positionTitle = context.positionTitle();
        String resumeContent = context.resumeContent();
        String previousReviewNotes = context.previousReviewNotes();
        // 从状态获取前序节点产出的中间结果
        String companyResearch = (String) state.value(STATE_COMPANY_RESEARCH_RESULT).orElse("{}");
        String jdAnalysis = (String) state.value(STATE_JD_ANALYSIS_RESULT).orElse("{}");
        // 获取AI提示词配置并构建用户提示词
        AIPromptProperties.PromptConfig config = aiPromptProperties.getPreparationGraph().getGeneratePreparationConfig();
        String userPrompt = config.getUserPromptTemplate()
                .replace("{companyName}", companyName)
                .replace("{positionTitle}", positionTitle)
                .replace("{jdAnalysis}", jdAnalysis)
                .replace("{resumeContent}", resumeContent.isEmpty() ? "（未提供简历）" : resumeContent)
                .replace("{previousReviewNotes}", previousReviewNotes.isEmpty() ? "（无上一轮复盘笔记）" : previousReviewNotes);
        // 调用AI生成准备事项（使用包装类约束返回格式）
        GeneratePreparationResult result = ChatClientHelper.callAndParse(
                chatClient, config.getSystemPrompt(), userPrompt, GeneratePreparationResult.class
        );
        // 如果AI返回空结果，直接返回空结果
        if (result == null || result.getItems() == null || result.getItems().isEmpty()) {
            log.warn("AI未返回准备事项");
            return buildEmptyResult();
        }
        // 不再直接保存到数据库，只返回 DTO 供前端预览
        // 前端用户确认后再调用 batchAddPreparations API 保存
        List<PreparationItemResult> preparationItems = result.getItems();
        log.info("准备事项生成完成: 共{}项（等待前端确认保存）", preparationItems.size());
        // 构建节点输出，用于SSE推送到前端
        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put(OUTPUT_NODE, NODE_GENERATE_PREPARATION);
        nodeOutput.put(OUTPUT_PROGRESS, 100);
        nodeOutput.put(OUTPUT_MESSAGE, "准备事项生成完成");
        nodeOutput.put(OUTPUT_DATA, preparationItems);
        // 构建并返回节点结果
        Map<String, Object> nodeResult = new HashMap<>();
        nodeResult.put(STATE_PREPARATION_ITEMS, preparationItems);
        nodeResult.put(STATE_MESSAGES, List.of("完成准备事项生成"));
        nodeResult.put(STATE_NODE_OUTPUT, nodeOutput);
        return nodeResult;
    }

    /**
     * 构建空结果（面试ID为空或AI返回空结果时使用）
     *
     * @return 空的节点结果
     */
    private Map<String, Object> buildEmptyResult() {
        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put(OUTPUT_NODE, NODE_GENERATE_PREPARATION);
        nodeOutput.put(OUTPUT_PROGRESS, 100);
        nodeOutput.put(OUTPUT_MESSAGE, "准备事项生成完成");
        nodeOutput.put(OUTPUT_DATA, List.of());
        Map<String, Object> emptyResult = new HashMap<>();
        emptyResult.put(STATE_PREPARATION_ITEMS, List.of());
        emptyResult.put(STATE_MESSAGES, List.of("完成准备事项生成"));
        emptyResult.put(STATE_NODE_OUTPUT, nodeOutput);
        return emptyResult;
    }

}
