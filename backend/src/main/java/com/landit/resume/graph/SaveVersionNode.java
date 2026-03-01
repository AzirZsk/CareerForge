package com.landit.resume.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.landit.resume.graph.ResumeOptimizeGraphConstants.*;

/**
 * 保存版本节点
 * 保存优化后的简历版本
 *
 * @author Azir
 */
@Slf4j
public class SaveVersionNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 保存版本 ===");

        String resumeId = state.value(STATE_RESUME_ID).map(v -> (String) v).orElse("");
        String optimizedSections = state.value(STATE_OPTIMIZED_SECTIONS).map(v -> (String) v).orElse(DEFAULT_EMPTY_JSON);

        // 获取优化前后的分数
        Integer improvementScore = state.value(STATE_IMPROVEMENT_SCORE).map(v -> (Integer) v).orElse(0);
        Integer originalScore = state.value(STATE_OVERALL_SCORE).map(v -> (Integer) v).orElse(0);
        Integer newScore = originalScore + improvementScore;

        // TODO: 调用 ResumeService 保存版本
        log.info("保存简历版本: resumeId={}", resumeId);

        // 模拟版本号
        String versionId = "v" + System.currentTimeMillis();
        String versionName = "优化版本_" + new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());

        List<String> messages = new ArrayList<>();
        messages.add("简历版本保存成功");
        messages.add("优化工作流完成");

        // 构建节点输出数据（用于 SSE）
        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put(OUTPUT_NODE, NODE_SAVE_VERSION);
        nodeOutput.put(OUTPUT_PROGRESS, 100);
        nodeOutput.put(OUTPUT_MESSAGE, "优化完成！简历已保存");
        nodeOutput.put(OUTPUT_DATA, Map.of(
                STATE_STATUS, STATUS_COMPLETED,
                "resumeId", resumeId,
                STATE_VERSION_ID, versionId,
                STATE_VERSION_NAME, versionName,
                "originalScore", originalScore,
                "newScore", newScore,
                "improvementScore", improvementScore
        ));

        return Map.of(
                STATE_STATUS, STATUS_COMPLETED,
                STATE_MESSAGES, messages,
                STATE_CURRENT_STEP, NODE_SAVE_VERSION,
                STATE_NODE_OUTPUT, nodeOutput,
                STATE_VERSION_ID, versionId,
                STATE_VERSION_NAME, versionName
        );
    }
}
