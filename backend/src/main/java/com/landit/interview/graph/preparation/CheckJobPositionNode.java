package com.landit.interview.graph.preparation;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.landit.jobposition.entity.JobPosition;
import com.landit.jobposition.service.JobPositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.landit.interview.graph.preparation.InterviewPreparationGraphConstants.*;

/**
 * 检查职位节点
 * 检查职位是否存在，JD分析是否有效
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CheckJobPositionNode implements NodeAction {

    private final JobPositionService jobPositionService;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始检查职位 ===");
        String companyId = (String) state.value(STATE_COMPANY_ID).orElse(null);
        String positionTitle = (String) state.value(STATE_POSITION_TITLE).orElse(null);
        boolean needAnalysis = true;
        String jobPositionId = null;
        if (companyId != null && positionTitle != null && !positionTitle.isEmpty()) {
            JobPosition jobPosition = jobPositionService.findByCompanyIdAndTitle(companyId, positionTitle).orElse(null);
            if (jobPosition != null) {
                jobPositionId = String.valueOf(jobPosition.getId());
                needAnalysis = !jobPositionService.hasValidAnalysis(jobPosition);
                log.info("职位已存在: id={}, 需要分析: {}", jobPositionId, needAnalysis);
            } else {
                log.info("职位不存在，需要创建并分析");
            }
        } else {
            needAnalysis = false;
            log.info("缺少公司ID或职位名称，跳过JD分析");
        }
        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put(OUTPUT_NODE, NODE_CHECK_JOB_POSITION);
        nodeOutput.put(OUTPUT_PROGRESS, 40);
        nodeOutput.put(OUTPUT_MESSAGE, needAnalysis ? "职位JD需要分析" : "职位信息已存在");
        Map<String, Object> result = new HashMap<>();
        result.put(STATE_JOB_POSITION_ID, jobPositionId);
        result.put(STATE_NEED_JD_ANALYSIS, needAnalysis);
        result.put(STATE_NODE_OUTPUT, nodeOutput);
        return result;
    }

}
