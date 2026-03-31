package com.landit.interview.graph.preparation;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.landit.common.enums.PreparationItemType;
import com.landit.common.enums.PreparationSource;
import com.landit.common.util.ChatClientHelper;
import com.landit.interview.entity.InterviewPreparation;
import com.landit.interview.service.InterviewPreparationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始生成准备事项 ===");
        String interviewId = (String) state.value(STATE_INTERVIEW_ID).orElse(null);
        String companyName = (String) state.value(STATE_COMPANY_NAME).orElse(null);
        String positionTitle = (String) state.value(STATE_POSITION_TITLE).orElse(null);
        String companyResearch = (String) state.value(STATE_COMPANY_RESEARCH_RESULT).orElse("{}");
        String jdAnalysis = (String) state.value(STATE_JD_ANALYSIS_RESULT).orElse("{}");
        String systemPrompt = """
                你是一位专业的面试准备顾问。请根据公司调研信息和JD分析结果，生成面试准备事项。

                每个准备事项应包含：
                - type: 类型（COMPANY_RESEARCH/JD_KEYWORDS/TECH_PREP/BEHAVIORAL/CASE_STUDY）
                - title: 标题（简洁明了）
                - content: 具体内容（详细说明需要准备什么）

                类型说明：
                - COMPANY_RESEARCH: 公司相关研究（了解公司业务、文化、最新动态）
                - JD_KEYWORDS: JD关键词准备（简历和面试中需要体现的关键词）
                - TECH_PREP: 技术准备（需要复习的技术知识点）
                - BEHAVIORAL: 行为面试准备（STAR法则准备的行为问题）
                - CASE_STUDY: 案例准备（可以分享的项目案例）

                请生成5-10个准备事项，以JSON数组格式返回。
                """;
        String userPrompt = String.format("""
                公司名称：%s
                职位名称：%s

                公司调研结果：
                %s

                JD分析结果：
                %s

                请生成面试准备事项。
                """,
                companyName != null ? companyName : "未知",
                positionTitle != null ? positionTitle : "未知",
                companyResearch,
                jdAnalysis
        );
        List<Map<String, String>> preparationItems = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, List.class
        );
        List<InterviewPreparation> savedPreparations = new ArrayList<>();
        int sortOrder = 0;
        if (preparationItems != null && interviewId != null) {
            for (Map<String, String> item : preparationItems) {
                InterviewPreparation preparation = new InterviewPreparation();
                preparation.setInterviewId(interviewId);
                String typeStr = item.get("type");
                PreparationItemType type = PreparationItemType.fromCode(typeStr);
                if (type != null) {
                    preparation.setItemType(type.getCode());
                } else {
                    preparation.setItemType(PreparationItemType.TODO.getCode());
                }
                preparation.setTitle(item.get("title"));
                preparation.setContent(item.get("content"));
                preparation.setCompleted(false);
                preparation.setSource(PreparationSource.AI_GENERATED.name());
                preparation.setSortOrder(sortOrder++);
                preparationService.save(preparation);
                savedPreparations.add(preparation);
            }
        }
        log.info("准备事项生成完成: 数量={}", savedPreparations.size());
        List<String> messages = new ArrayList<>();
        messages.add("完成准备事项生成");
        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put(OUTPUT_NODE, NODE_GENERATE_PREPARATION);
        nodeOutput.put(OUTPUT_PROGRESS, 100);
        nodeOutput.put(OUTPUT_MESSAGE, "准备事项生成完成");
        nodeOutput.put(OUTPUT_DATA, savedPreparations);
        Map<String, Object> result = new HashMap<>();
        result.put(STATE_PREPARATION_ITEMS, savedPreparations);
        result.put(STATE_MESSAGES, messages);
        result.put(STATE_NODE_OUTPUT, nodeOutput);
        return result;
    }

}
