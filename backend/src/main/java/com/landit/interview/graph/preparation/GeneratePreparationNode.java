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
        PreparationContext context = extractContext(state);
        List<InterviewPreparation> savedPreparations = generateAndSavePreparations(context);
        log.info("准备事项生成完成: 数量={}", savedPreparations.size());
        return buildResult(savedPreparations);
    }

    private PreparationContext extractContext(OverAllState state) {
        return new PreparationContext(
                (String) state.value(STATE_INTERVIEW_ID).orElse(null),
                (String) state.value(STATE_COMPANY_NAME).orElse("未知"),
                (String) state.value(STATE_POSITION_TITLE).orElse("未知"),
                (String) state.value(STATE_COMPANY_RESEARCH_RESULT).orElse("{}"),
                (String) state.value(STATE_JD_ANALYSIS_RESULT).orElse("{}")
        );
    }

    private List<InterviewPreparation> generateAndSavePreparations(PreparationContext context) {
        if (context.interviewId() == null) {
            return new ArrayList<>();
        }
        AIPromptProperties.PromptConfig config = aiPromptProperties.getPreparationGraph().getGeneratePreparationConfig();
        String systemPrompt = config.getSystemPrompt();
        String userPrompt = config.getUserPromptTemplate()
                .replace("{companyName}", context.companyName())
                .replace("{positionTitle}", context.positionTitle())
                .replace("{companyResearch}", context.companyResearch())
                .replace("{jdAnalysis}", context.jdAnalysis());
        List<Map<String, Object>> preparationItems = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, List.class
        );
        if (preparationItems == null || preparationItems.isEmpty()) {
            return new ArrayList<>();
        }
        return savePreparationItems(context.interviewId(), preparationItems);
    }

    private List<InterviewPreparation> savePreparationItems(
            String interviewId,
            List<Map<String, Object>> preparationItems
    ) {
        List<InterviewPreparation> savedPreparations = new ArrayList<>();
        int sortOrder = 0;
        for (Map<String, Object> item : preparationItems) {
            InterviewPreparation preparation = convertToEntity(interviewId, item, sortOrder++);
            preparationService.save(preparation);
            savedPreparations.add(preparation);
        }
        return savedPreparations;
    }

    private InterviewPreparation convertToEntity(
            String interviewId,
            Map<String, Object> item,
            int sortOrder
    ) {
        InterviewPreparation preparation = new InterviewPreparation();
        preparation.setInterviewId(interviewId);
        preparation.setItemType(resolveItemType((String) item.get("type")));
        preparation.setTitle((String) item.get("title"));
        preparation.setContent((String) item.get("content"));
        preparation.setCompleted(false);
        preparation.setSource(PreparationSource.AI_GENERATED.getCode());
        preparation.setPriority(resolvePriority((String) item.get("priority")));
        preparation.setResources(serializeResources(item.get("resources")));
        preparation.setSortOrder(sortOrder);
        return preparation;
    }

    private String resolveItemType(String typeStr) {
        PreparationItemType itemType = PreparationItemType.fromCode(typeStr);
        return itemType != null ? itemType.getCode() : PreparationItemType.TODO.getCode();
    }

    private String resolvePriority(String priorityStr) {
        PreparationPriority priority = PreparationPriority.fromCode(priorityStr);
        return priority != null ? priority.getCode() : PreparationPriority.RECOMMENDED.getCode();
    }

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

    private Map<String, Object> buildResult(List<InterviewPreparation> savedPreparations) {
        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put(OUTPUT_NODE, NODE_GENERATE_PREPARATION);
        nodeOutput.put(OUTPUT_PROGRESS, 100);
        nodeOutput.put(OUTPUT_MESSAGE, "准备事项生成完成");
        nodeOutput.put(OUTPUT_DATA, savedPreparations);
        Map<String, Object> result = new HashMap<>();
        result.put(STATE_PREPARATION_ITEMS, savedPreparations);
        result.put(STATE_MESSAGES, List.of("完成准备事项生成"));
        result.put(STATE_NODE_OUTPUT, nodeOutput);
        return result;
    }

    private record PreparationContext(
            String interviewId,
            String companyName,
            String positionTitle,
            String companyResearch,
            String jdAnalysis
    ) {}

}
