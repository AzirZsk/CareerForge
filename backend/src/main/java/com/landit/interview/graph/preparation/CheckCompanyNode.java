package com.landit.interview.graph.preparation;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.landit.company.entity.Company;
import com.landit.company.service.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.landit.interview.graph.preparation.InterviewPreparationGraphConstants.*;

/**
 * 检查公司节点
 * 检查公司是否存在，调研是否过期
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CheckCompanyNode implements NodeAction {

    private final CompanyService companyService;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始检查公司 ===");
        String companyName = (String) state.value(STATE_COMPANY_NAME).orElse(null);
        boolean needResearch = true;
        String companyId = null;
        if (companyName != null && !companyName.isEmpty()) {
            Company company = companyService.findByName(companyName).orElse(null);
            if (company != null) {
                companyId = String.valueOf(company.getId());
                needResearch = companyService.isResearchExpired(company);
                log.info("公司已存在: id={}, 需要调研: {}", companyId, needResearch);
            } else {
                log.info("公司不存在，需要创建并调研");
            }
        }
        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put(OUTPUT_NODE, NODE_CHECK_COMPANY);
        nodeOutput.put(OUTPUT_PROGRESS, 10);
        nodeOutput.put(OUTPUT_MESSAGE, needResearch ? "公司需要调研" : "公司信息已存在");
        Map<String, Object> result = new HashMap<>();
        result.put(STATE_COMPANY_ID, companyId);
        result.put(STATE_NEED_COMPANY_RESEARCH, needResearch);
        result.put(STATE_NODE_OUTPUT, nodeOutput);
        return result;
    }

}
