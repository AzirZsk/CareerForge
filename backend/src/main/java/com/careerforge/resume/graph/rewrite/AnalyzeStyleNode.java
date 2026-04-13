package com.careerforge.resume.graph.rewrite;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.careerforge.common.config.AIPromptProperties;
import com.careerforge.common.util.ChatClientHelper;
import com.careerforge.common.util.JsonParseHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.careerforge.resume.graph.rewrite.RewriteGraphConstants.*;

/**
 * 风格分析节点
 * 解析参考简历，提取写作风格特征（语气、句式、动词习惯、量化密度等）
 * 输入为参考简历的JSON字符串（来自文件解析结果）
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class AnalyzeStyleNode implements NodeAction {

    private final ChatClient chatClient;
    private final AIPromptProperties aiPromptProperties;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始分析参考简历风格 ===");
        // 获取参考简历（JSON字符串，来自文件解析）
        String referenceContent = (String) state.value(STATE_REFERENCE_CONTENT).orElse(null);
        Objects.requireNonNull(referenceContent, "参考简历内容不能为空");
        // 调用AI分析风格
        AIPromptProperties.PromptConfig promptConfig = aiPromptProperties.getRewriteGraph().getAnalyzeStyleConfig();
        String systemPrompt = promptConfig.getSystemPrompt();
        String userPrompt = ChatClientHelper.renderTemplate(
                promptConfig.getUserPromptTemplate(),
                Map.of("referenceSections", referenceContent)
        );
        StyleAnalysisResponse response = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, StyleAnalysisResponse.class
        );
        log.info("风格分析完成: tone={}", response.getToneDescription());
        String analysisJson = JsonParseHelper.toJsonString(response);
        List<String> messages = new ArrayList<>();
        messages.add("参考简历风格分析完成");
        // 构建节点输出数据（用于SSE）
        Map<String, Object> nodeOutput = JsonParseHelper.buildNodeOutput(
                NODE_ANALYZE_STYLE,
                30,
                "参考简历风格分析完成",
                response
        );

        return Map.of(
                STATE_STYLE_ANALYSIS, analysisJson,
                STATE_MESSAGES, messages,
                STATE_CURRENT_STEP, NODE_ANALYZE_STYLE,
                STATE_NODE_OUTPUT, nodeOutput
        );
    }
}
