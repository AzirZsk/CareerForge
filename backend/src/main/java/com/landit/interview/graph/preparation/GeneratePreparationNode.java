package com.landit.interview.graph.preparation;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final InterviewPreparationService preparationService;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            你是一位专业的面试准备顾问。请根据公司调研信息和JD分析结果，生成面试准备事项。

            每个准备事项必须包含：
            - type: 类型，必须使用以下小写值之一：
              - company_research: 公司相关研究（了解公司业务、文化、最新动态）
              - jd_keywords: JD关键词准备（简历和面试中需要体现的关键词）
              - tech_prep: 技术准备（需要复习的技术知识点）
              - behavioral: 行为面试准备（STAR法则准备的行为问题）
              - case_study: 案例准备（可以分享的项目案例）
            - title: 标题（简洁明了，不超过50字）
            - content: 具体内容（详细说明需要准备什么）
            - priority: 优先级（required必做/recommended推荐/optional可选）
            - resources: 关联资源数组（可选）

            优先级分布要求（共5-8个事项）：
            - required（必做）：约50%，核心准备项
            - recommended（推荐）：约35%，加分项
            - optional（可选）：约15%，锦上添花

            资源格式（可选）：
            - type: link/note/code/video
            - title: 资源标题
            - url: 链接地址（type为link时必填）

            示例输出：
            ```json
            [
              {
                "type": "company_research",
                "title": "了解公司核心业务模式",
                "content": "研究公司的主要产品和服务...",
                "priority": "required",
                "resources": [
                  {"type": "link", "title": "公司官网", "url": "https://example.com"}
                ]
              }
            ]
            ```

            请严格按JSON数组格式返回，不要添加任何其他文字。
            """;

    private static final String USER_PROMPT_TEMPLATE = """
            公司名称：%s
            职位名称：%s

            公司调研结果：
            %s

            JD分析结果：
            %s

            请生成5-8个面试准备事项，确保优先级分布合理。
            """;

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
        String userPrompt = buildUserPrompt(context);
        List<Map<String, Object>> preparationItems = ChatClientHelper.callAndParse(
                chatClient, SYSTEM_PROMPT, userPrompt, List.class
        );
        if (preparationItems == null || preparationItems.isEmpty()) {
            return new ArrayList<>();
        }
        return savePreparationItems(context.interviewId(), preparationItems);
    }

    private String buildUserPrompt(PreparationContext context) {
        return String.format(
                USER_PROMPT_TEMPLATE,
                context.companyName(),
                context.positionTitle(),
                context.companyResearch(),
                context.jdAnalysis()
        );
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
