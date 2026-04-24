package com.careerforge.resume.graph.rewrite;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.careerforge.common.config.prompt.RewritePromptProperties;
import com.careerforge.common.config.prompt.PromptConfig;
import com.careerforge.common.schema.GraphSchemaRegistry;
import com.careerforge.common.util.ChatClientHelper;
import com.careerforge.common.util.JsonParseHelper;
import com.careerforge.resume.dto.ResumeDetailVO;
import com.careerforge.resume.util.ResumeSectionShortener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.careerforge.resume.graph.rewrite.RewriteGraphConstants.*;

/**
 * 风格差异生成节点
 * 对比用户简历与参考风格，生成风格改写策略（复用 SuggestionsResponse 格式）
 * 同时为用户简历生成短ID映射表
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class GenerateStyleDiffNode implements NodeAction {

    private final ChatClient chatClient;
    private final RewritePromptProperties promptProperties;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始生成风格差异建议 ===");
        // 获取用户简历
        ResumeDetailVO resumeDetail = (ResumeDetailVO) state.value(STATE_RESUME_CONTENT).orElse(null);
        Objects.requireNonNull(resumeDetail, "用户简历内容不能为空");
        // 获取风格分析结果
        String styleAnalysis = (String) state.value(STATE_STYLE_ANALYSIS).orElse(DEFAULT_EMPTY_JSON);
        // 短ID化处理用户简历（生成短ID映射表，供RewriteSectionNode使用）
        ResumeSectionShortener.ShortenResult shortenResult = ResumeSectionShortener.shorten(resumeDetail);
        String resumeSections = shortenResult.getShortenedContent();
        // 调用AI生成风格差异策略
        PromptConfig promptConfig = promptProperties.getGenerateStyleDiffConfig();
        String systemPrompt = promptConfig.getSystemPrompt();
        String userPrompt = ChatClientHelper.renderTemplate(
                promptConfig.getUserPromptTemplate(),
                Map.of("styleAnalysis", styleAnalysis, "resumeSections", resumeSections)
        );
        GraphSchemaRegistry.SuggestionsResponse response = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, GraphSchemaRegistry.SuggestionsResponse.class
        );
        String strategiesJson = JsonParseHelper.toJsonString(response);
        log.info("风格差异建议生成完成, 共 {} 条策略",
                response.getSuggestions() != null ? response.getSuggestions().size() : 0);
        List<String> messages = new ArrayList<>();
        messages.add("风格差异建议生成完成");
        // 构建节点输出数据（用于SSE）
        Map<String, Object> nodeOutput = JsonParseHelper.buildNodeOutput(
                NODE_GENERATE_STYLE_DIFF,
                55,
                "风格差异建议生成完成",
                response
        );

        return Map.of(
                STATE_OPTIMIZED_SECTIONS, strategiesJson,
                STATE_SECTION_ID_MAP, shortenResult.getShortIdToRealIdMap(),
                STATE_MESSAGES, messages,
                STATE_CURRENT_STEP, NODE_GENERATE_STYLE_DIFF,
                STATE_NODE_OUTPUT, nodeOutput
        );
    }
}
