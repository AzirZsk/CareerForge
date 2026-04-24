package com.careerforge.resume.graph.optimize;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.careerforge.common.config.prompt.ResumeOptimizePromptProperties;
import com.careerforge.common.config.prompt.PromptConfig;
import com.careerforge.common.schema.GraphSchemaRegistry;
import com.careerforge.common.util.ChatClientHelper;
import com.careerforge.common.util.JsonParseHelper;
import com.careerforge.resume.dto.DiagnoseResumeResponse;
import com.careerforge.resume.dto.ResumeDetailVO;
import com.careerforge.resume.service.ResumeSuggestionService;
import com.careerforge.resume.util.ResumeSectionShortener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.careerforge.resume.graph.optimize.ResumeOptimizeGraphConstants.*;

/**
 * 生成优化建议节点
 * 基于诊断结果生成具体的优化建议，使用 JSON Schema 约束输出格式
 * 使用拆分提示词方式调用以利用前缀缓存优化
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class GenerateSuggestionsNode implements NodeAction {

    private final ChatClient chatClient;
    private final ResumeOptimizePromptProperties promptProperties;
    private final ResumeSuggestionService resumeSuggestionService;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 生成优化建议 ===");

        String diagnosisResult = state.value(STATE_DIAGNOSIS_RESULT).map(v -> (String) v).orElse(DEFAULT_EMPTY_JSON);
        ResumeDetailVO resumeDetail = state.value(STATE_RESUME_CONTENT)
                .map(v -> (ResumeDetailVO) v)
                .orElse(null);
        // 使用简短标识符替代雪花 ID，避免 AI 幻觉
        String resumeContentJson = resumeDetail != null
                ? ResumeSectionShortener.shorten(resumeDetail).getShortenedContent()
                : DEFAULT_EMPTY_JSON;

        // 使用拆分提示词调用（前缀缓存优化）
        PromptConfig promptConfig = promptProperties.getGenerateSuggestionsConfig();
        String systemPrompt = promptConfig.getSystemPrompt();
        String userPrompt = ChatClientHelper.renderTemplate(
                promptConfig.getUserPromptTemplate(),
                Map.of("diagnosisResult", diagnosisResult, "resumeContent", resumeContentJson)
        );

        // 调用 AI 并自动解析（带重试）
        GraphSchemaRegistry.SuggestionsResponse response = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, GraphSchemaRegistry.SuggestionsResponse.class
        );
        String suggestionsResult = JsonParseHelper.toJsonString(response);

        log.info("优化建议生成完成");

        List<DiagnoseResumeResponse.Suggestion> suggestions = response.getSuggestions() != null
                ? response.getSuggestions()
                : List.of();
        int suggestionCount = suggestions.size();

        // 保存建议到数据库
        String resumeId = resumeDetail != null ? resumeDetail.getId() : null;
        @SuppressWarnings("unchecked")
        Map<String, String> shortIdToRealIdMap = (Map<String, String>) state
                .value(STATE_SECTION_ID_MAP)
                .orElse(Map.of());

        if (resumeId != null && !suggestions.isEmpty()) {
            try {
                resumeSuggestionService.saveSuggestions(resumeId, suggestions, shortIdToRealIdMap);
                log.info("优化建议已保存到数据库: resumeId={}, count={}", resumeId, suggestionCount);
            } catch (Exception e) {
                log.error("保存优化建议失败: resumeId={}", resumeId, e);
            }
        }

        List<String> messages = new ArrayList<>();
        messages.add("生成优化建议完成");
        messages.add("共生成 " + suggestionCount + " 条建议");

        // 构建节点输出数据（用于 SSE）
        Map<String, Object> nodeOutput = JsonParseHelper.buildNodeOutput(
                NODE_GENERATE_SUGGESTIONS,
                65,
                "已生成 " + suggestionCount + " 条优化建议",
                Map.of(
                        "suggestions", suggestions,
                        "quickWins", response.getQuickWins(),
                        "estimatedImprovement", response.getEstimatedImprovement(),
                        "totalSuggestions", suggestionCount
                )
        );

        return Map.of(
                STATE_SUGGESTIONS, suggestionsResult,
                STATE_MESSAGES, messages,
                STATE_CURRENT_STEP, NODE_GENERATE_SUGGESTIONS,
                STATE_NODE_OUTPUT, nodeOutput,
                STATE_SUGGESTION_LIST, suggestions,
                STATE_QUICK_WINS, response.getQuickWins()
        );
    }
}
