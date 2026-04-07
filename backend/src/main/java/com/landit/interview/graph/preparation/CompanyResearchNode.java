package com.landit.interview.graph.preparation;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.util.ChatClientHelper;
import com.landit.common.util.JsonParseHelper;
import com.landit.company.dto.CompanyResearchResult;
import com.landit.company.entity.Company;
import com.landit.company.service.CompanyService;
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
 * 公司调研节点
 * 检查公司是否存在且调研是否有效，如有效则使用缓存，否则执行AI调研
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyResearchNode implements NodeAction {

    private final ChatClient chatClient;
    private final AIPromptProperties aiPromptProperties;
    private final CompanyService companyService;
    private final PreparationContextBuilder contextBuilder;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始公司调研 ===");
        // 从工作流状态中获取面试ID，通过 ContextBuilder 获取公司名称
        String interviewId = (String) state.value(STATE_INTERVIEW_ID).orElse(null);
        PreparationContextBuilder.PreparationContext context = contextBuilder.buildContext(interviewId);
        String companyName = context.companyName();

        // 如果公司名称为空，跳过调研
        if (companyName == null || companyName.isEmpty()) {
            log.info("公司名称为空，跳过公司调研");
            return buildSkippedResult();
        }

        // 检查公司是否存在且调研是否有效
        Company existingCompany = companyService.findByName(companyName).orElse(null);
        if (existingCompany != null && !companyService.isResearchExpired(existingCompany)) {
            log.info("公司调研已存在且有效，使用缓存: companyId={}", existingCompany.getId());
            return buildCachedResult(existingCompany);
        }

        // 需要执行AI调研
        log.info("执行AI公司调研: {}", companyName);
        return executeAIRresearch(companyName);
    }

    /**
     * 执行AI调研并持久化结果
     */
    private Map<String, Object> executeAIRresearch(String companyName) {
        // 获取AI提示词配置
        AIPromptProperties.PromptConfig config = aiPromptProperties.getPreparationGraph().getCompanyResearchConfig();
        String systemPrompt = config.getSystemPrompt();
        String userPrompt = config.getUserPromptTemplate().replace("{companyName}", companyName);

        // 调用AI生成公司调研报告
        CompanyResearchResult researchResult = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, CompanyResearchResult.class
        );

        // 持久化调研结果到数据库
        String researchJson = JsonParseHelper.toJsonString(researchResult);
        Company company = companyService.createOrUpdateCompany(companyName, researchJson);
        String companyId = String.valueOf(company.getId());

        log.info("AI公司调研完成: companyId={}", companyId);

        // 构建节点输出
        List<String> messages = new ArrayList<>();
        messages.add("完成公司调研: " + companyName);

        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put(OUTPUT_NODE, NODE_COMPANY_RESEARCH);
        nodeOutput.put(OUTPUT_PROGRESS, 30);
        nodeOutput.put(OUTPUT_MESSAGE, "公司调研完成");
        nodeOutput.put(OUTPUT_DATA, Map.of("companyResearch", researchResult));

        Map<String, Object> result = new HashMap<>();
        result.put(STATE_COMPANY_ID, companyId);
        result.put(STATE_COMPANY_RESEARCH_RESULT, researchJson);
        result.put(STATE_MESSAGES, messages);
        result.put(STATE_NODE_OUTPUT, nodeOutput);
        return result;
    }

    /**
     * 构建缓存结果（调研已存在且有效时使用）
     */
    private Map<String, Object> buildCachedResult(Company company) {
        String companyId = String.valueOf(company.getId());
        String researchJson = company.getResearch();

        List<String> messages = new ArrayList<>();
        messages.add("公司调研已存在（使用缓存）");

        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put(OUTPUT_NODE, NODE_COMPANY_RESEARCH);
        nodeOutput.put(OUTPUT_PROGRESS, 30);
        nodeOutput.put(OUTPUT_MESSAGE, "公司调研完成（使用缓存）");
        nodeOutput.put(OUTPUT_CACHED, true);  // 标识使用缓存

        // 缓存数据也解析出来用于前端展示
        CompanyResearchResult researchResult = JsonParseHelper.parseToEntity(researchJson, CompanyResearchResult.class);
        if (researchResult != null) {
            nodeOutput.put(OUTPUT_DATA, Map.of("companyResearch", researchResult));
        }

        Map<String, Object> result = new HashMap<>();
        result.put(STATE_COMPANY_ID, companyId);
        result.put(STATE_COMPANY_RESEARCH_RESULT, researchJson != null ? researchJson : "{}");
        result.put(STATE_MESSAGES, messages);
        result.put(STATE_NODE_OUTPUT, nodeOutput);
        return result;
    }

    /**
     * 构建跳过结果（公司名称为空时使用）
     */
    private Map<String, Object> buildSkippedResult() {
        List<String> messages = new ArrayList<>();
        messages.add("跳过公司调研（未提供公司名称）");

        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put(OUTPUT_NODE, NODE_COMPANY_RESEARCH);
        nodeOutput.put(OUTPUT_PROGRESS, 30);
        nodeOutput.put(OUTPUT_MESSAGE, "跳过公司调研");

        Map<String, Object> result = new HashMap<>();
        result.put(STATE_COMPANY_ID, null);
        result.put(STATE_COMPANY_RESEARCH_RESULT, "{}");
        result.put(STATE_MESSAGES, messages);
        result.put(STATE_NODE_OUTPUT, nodeOutput);
        return result;
    }

}
