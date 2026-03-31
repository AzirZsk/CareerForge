package com.landit.interview.graph.review;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.landit.common.util.ChatClientHelper;
import com.landit.common.util.JsonParseHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.landit.interview.graph.review.ReviewAnalysisGraphConstants.*;

/**
 * 生成改进建议节点
 * 根据分析结果生成具体的改进建议
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GenerateAdviceNode implements NodeAction {

    private final ChatClient chatClient;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始生成改进建议 ===");
        String analysisResult = (String) state.value(STATE_ANALYSIS_RESULT).orElse("{}");
        String systemPrompt = """
                你是一位专业的职业发展顾问。请根据面试分析结果，生成具体的改进建议：

                建议类型：
                1. 技能提升（需要学习或加强的技能）
                2. 面试技巧（面试表现方面的建议）
                3. 项目经验（项目介绍方面的优化建议）
                4. 行为面试（STAR法则的应用建议）
                5. 后续行动（具体的行动计划）

                请以JSON数组格式返回建议列表，每条建议包含：
                - category: 建议类别
                - title: 建议标题
                - description: 详细描述
                - priority: 优先级（高/中/低）
                - actionItems: 具体行动项列表
                """;
        String userPrompt = "请根据以下分析结果生成改进建议：\n" + analysisResult;
        String adviceJson = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, String.class
        );
        log.info("改进建议生成完成");
        List<String> messages = new ArrayList<>();
        messages.add("完成改进建议生成");
        Map<String, Object> nodeOutput = new HashMap<>();
        nodeOutput.put(OUTPUT_NODE, NODE_GENERATE_ADVICE);
        nodeOutput.put(OUTPUT_PROGRESS, 100);
        nodeOutput.put(OUTPUT_MESSAGE, "改进建议生成完成");
        nodeOutput.put(OUTPUT_DATA, adviceJson);
        Map<String, Object> result = new HashMap<>();
        result.put(STATE_ADVICE_LIST, adviceJson);
        result.put(STATE_MESSAGES, messages);
        result.put(STATE_NODE_OUTPUT, nodeOutput);
        return result;
    }

}
