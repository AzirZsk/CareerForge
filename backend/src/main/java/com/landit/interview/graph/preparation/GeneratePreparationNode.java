package com.landit.interview.graph.preparation;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.enums.PreparationItemType;
import com.landit.common.enums.PreparationPriority;
import com.landit.common.enums.PreparationSource;
import com.landit.common.util.ChatClientHelper;
import com.landit.interview.entity.InterviewPreparation;
import com.landit.interview.service.InterviewPreparationService;
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
 * 生成准备事项节点
 * 根据公司调研和JD分析结果，生成面试准备事项
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeneratePreparationNode implements NodeAction {

    private final ChatClient chatClient;
    private final AIPromptProperties aiPromptProperties;
    private final InterviewPreparationService preparationService;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始生成准备事项 ===");
        // 从工作流状态中提取上下文信息
        String interviewId = (String) state.value(STATE_INTERVIEW_ID).orElse(null);
        if (interviewId == null) {
            log.warn("面试ID为空，跳过准备事项生成");
            return buildEmptyResult();
        }
        String companyName = (String) state.value(STATE_COMPANY_NAME).orElse("");
        String positionTitle = (String) state.value(STATE_POSITION_TITLE).orElse("");
        String companyResearch = (String) state.value(STATE_COMPANY_RESEARCH_RESULT).orElse("{}");
        String jdAnalysis = (String) state.value(STATE_JD_ANALYSIS_RESULT).orElse("{}");
        // 获取AI提示词配置并构建用户提示词
        AIPromptProperties.PromptConfig config = aiPromptProperties.getPreparationGraph().getGeneratePreparationConfig();
        String userPrompt = config.getUserPromptTemplate()
                .replace("{companyName}", companyName)
                .replace("{positionTitle}", positionTitle)
                .replace("{companyResearch}", companyResearch)
                .replace("{jdAnalysis}", jdAnalysis);
        // 调用AI生成准备事项
        List<Map<String, Object>> preparationItems = ChatClientHelper.callAndParse(
                chatClient, config.getSystemPrompt(), userPrompt, List.class
        );
        // 如果AI返回空结果，直接返回空结果
        if (preparationItems == null || preparationItems.isEmpty()) {
            log.warn("AI未返回准备事项");
            return buildEmptyResult();
        }
        // 保存准备事项到数据库
        List<InterviewPreparation> savedPreparations = new ArrayList<>();
        int sortOrder = 0;
        for (Map<String, Object> item : preparationItems) {
            InterviewPreparation preparation = convertToEntity(interviewId, item, sortOrder++);
            preparationService.save(preparation);
            savedPreparations.add(preparation);
        }
        log.info("准备事项生成完成: 共{}项", savedPreparations.size());
        // 构建节点输出，用于SSE推送到前端
        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put(OUTPUT_NODE, NODE_GENERATE_PREPARATION);
        nodeOutput.put(OUTPUT_PROGRESS, 100);
        nodeOutput.put(OUTPUT_MESSAGE, "准备事项生成完成");
        nodeOutput.put(OUTPUT_DATA, savedPreparations);
        // 构建并返回节点结果
        Map<String, Object> result = new HashMap<>();
        result.put(STATE_PREPARATION_ITEMS, savedPreparations);
        result.put(STATE_MESSAGES, List.of("完成准备事项生成"));
        result.put(STATE_NODE_OUTPUT, nodeOutput);
        return result;
    }

    /**
     * 将Map格式的准备事项转换为实体对象
     *
     * @param interviewId 面试ID
     * @param item        AI返回的准备事项Map
     * @param sortOrder   排序序号
     * @return 准备事项实体
     */
    private InterviewPreparation convertToEntity(String interviewId, Map<String, Object> item, int sortOrder) {
        InterviewPreparation preparation = new InterviewPreparation();
        preparation.setInterviewId(interviewId);
        preparation.setTitle((String) item.get("title"));
        preparation.setContent((String) item.get("content"));
        // 解析类型（无效则使用默认值TODO）
        String itemTypeStr = (String) item.get("itemType");
        PreparationItemType itemType = PreparationItemType.fromCode(itemTypeStr);
        preparation.setItemType(itemType != null ? itemType.getCode() : PreparationItemType.TODO.getCode());
        // 解析优先级（无效则使用默认值RECOMMENDED）
        String priorityStr = (String) item.get("priority");
        PreparationPriority priority = PreparationPriority.fromCode(priorityStr);
        preparation.setPriority(priority != null ? priority.getCode() : PreparationPriority.RECOMMENDED.getCode());
        // 序列化资源
        preparation.setResources(serializeResources(item.get("resources")));
        preparation.setSortOrder(sortOrder);
        preparation.setCompleted(false);
        preparation.setSource(PreparationSource.AI_GENERATED.getCode());
        return preparation;
    }

    /**
     * 序列化资源对象为JSON字符串
     *
     * @param resourcesObj 资源对象
     * @return JSON字符串，序列化失败返回null
     */
    private String serializeResources(Object resourcesObj) {
        if (resourcesObj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(resourcesObj);
        } catch (JsonProcessingException e) {
            log.warn("序列化资源失败: {}", e.getMessage());
            return null;
        }
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
        Map<String, Object> result = new HashMap<>();
        result.put(STATE_PREPARATION_ITEMS, List.of());
        result.put(STATE_MESSAGES, List.of("完成准备事项生成"));
        result.put(STATE_NODE_OUTPUT, nodeOutput);
        return result;
    }

}
