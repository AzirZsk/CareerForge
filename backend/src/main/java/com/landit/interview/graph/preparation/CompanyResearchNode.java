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
 * 使用AI调研公司背景信息
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

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始公司调研 ===");
        String companyName = (String) state.value(STATE_COMPANY_NAME).orElse(null);
        String companyId = (String) state.value(STATE_COMPANY_ID).orElse(null);
        AIPromptProperties.PromptConfig config = aiPromptProperties.getPreparationGraph().getCompanyResearchConfig();
        String systemPrompt = config.getSystemPrompt();
        String userPrompt = config.getUserPromptTemplate().replace("{companyName}", companyName);
        CompanyResearchResult researchResult = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, CompanyResearchResult.class
        );
        String researchJson = JsonParseHelper.toJsonString(researchResult);
        Company company = companyService.createOrUpdateCompany(companyName, researchJson);
        if (companyId == null) {
            companyId = String.valueOf(company.getId());
        }
        log.info("公司调研完成: companyId={}", companyId);
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

}
