package com.landit.resume.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.landit.common.config.AIPromptProperties;
import com.landit.common.util.ChatClientHelper;
import com.landit.common.util.JsonParseHelper;
import com.landit.resume.dto.DiagnoseResumeResponse;
import com.landit.resume.dto.ResumeDetailVO;
import com.landit.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.landit.resume.graph.ResumeOptimizeGraphConstants.*;

/**
 * 简历诊断节点
 * 调用 AI 分析简历质量，使用 JSON Schema 约束输出格式
 *
 * @author Azir
 */
@Slf4j
@RequiredArgsConstructor
public class DiagnoseResumeNode implements NodeAction {

    private final ChatClient chatClient;
    private final AIPromptProperties aiPromptProperties;
    private final ResumeService resumeService;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("=== 开始简历诊断===");

        ResumeDetailVO resumeDetail = (ResumeDetailVO) state.value(STATE_RESUME_CONTENT).orElse(null);
        Objects.requireNonNull(resumeDetail);
        String targetPosition = resumeDetail.getTargetPosition() != null
                ? resumeDetail.getTargetPosition()
                : DEFAULT_TARGET_POSITION;

        // 使用拆分提示词调用（前缀缓存优化）
        AIPromptProperties.PromptConfig promptConfig = aiPromptProperties.getGraph().getDiagnoseQuickConfig();
        String systemPrompt = promptConfig.getSystemPrompt();
        // 构建结构化的简历内容（包含 sectionId）
        String structuredResumeContent = buildStructuredResumeContent(resumeDetail);
        String userPrompt = ChatClientHelper.renderTemplate(
                promptConfig.getUserPromptTemplate(),
                Map.of("targetPosition", targetPosition, "resumeContent", structuredResumeContent)
        );

        // 调用 AI 并自动解析（带重试）
        DiagnoseResumeResponse response = ChatClientHelper.callAndParse(
                chatClient, systemPrompt, userPrompt, DiagnoseResumeResponse.class
        );
        String diagnosisResult = JsonParseHelper.toJsonString(response);

        log.info("诊断完成");

        // 诊断完成后立即保存评分到数据库
        String resumeId = resumeDetail.getId();
        if (resumeId != null) {
            try {
                // 保存整体评分和维度评分
                resumeService.updateDiagnosisScores(resumeId, response.getOverallScore(), response.getDimensionScores());
                // 保存模块评分
                if (response.getSectionScores() != null && !response.getSectionScores().isEmpty()) {
                    resumeService.updateSectionScores(resumeId, response.getSectionScores());
                }
                log.info("诊断评分已保存: resumeId={}, overallScore={}", resumeId, response.getOverallScore());
            } catch (Exception e) {
                log.error("保存诊断评分失败: resumeId={}", resumeId, e);
            }
        }

        List<String> messages = new ArrayList<>();
        messages.add("完成简历诊断");

        // 构建节点输出数据（用于 SSE）
        Map<String, Object> nodeOutput = JsonParseHelper.buildNodeOutput(
                NODE_DIAGNOSE_QUICK,
                35,
                "简历诊断完成",
                response
        );

        return Map.of(
                STATE_DIAGNOSIS_RESULT, diagnosisResult,
                STATE_MESSAGES, messages,
                STATE_CURRENT_STEP, NODE_DIAGNOSE_QUICK,
                STATE_NODE_OUTPUT, nodeOutput,
                STATE_OVERALL_SCORE, response.getOverallScore(),
                STATE_DIMENSIONS, response.getDimensionScores(),
                STATE_ISSUES, response.getWeaknesses(),
                STATE_HIGHLIGHTS, response.getStrengths(),
                STATE_QUICK_WINS, response.getQuickWins()
        );
    }

    /**
     * 构建结构化的简历内容，让 AI 知道每个 section 的 ID
     *
     * @param resumeDetail 简历详情
     * @return 结构化的内容字符串
     */
    private String buildStructuredResumeContent(ResumeDetailVO resumeDetail) {
        StringBuilder sb = new StringBuilder();
        List<ResumeDetailVO.ResumeSectionVO> sections = resumeDetail.getSections();
        if (sections == null || sections.isEmpty()) {
            return "";
        }

        for (ResumeDetailVO.ResumeSectionVO section : sections) {
            sb.append("## SECTION:").append(section.getId()).append("\n");
            sb.append("### ").append(section.getTitle() != null ? section.getTitle() : "").append("\n\n");

            // 添加内容描述
            Object content = section.getContent();
            if (content instanceof Map) {
                Map<String, Object> contentMap = (Map<String, Object>) content;
                sb.append("- 类型: ").append(section.getType()).append("\n");
                for (Map.Entry<String, Object> entry : contentMap.entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof String) {
                        sb.append(value.toString());
                    } else if (value instanceof Number) {
                        sb.append(value.toString());
                    } else if (value instanceof Boolean) {
                        sb.append(value.toString());
                    } else if (value instanceof List) {
                        sb.append(formatListValue((List<?>) value));
                    } else if (value instanceof Map) {
                        sb.append(formatMapValue((Map<String, Object>) value));
                    } else {
                        sb.append(String.valueOf(value));
                    }
                }
            } else if (content instanceof List) {
                List<?> items = (List<?>) content;
                sb.append("- 项目数量: ").append(items.size()).append("\n");
                for (Object item : items) {
                    if (item instanceof Map) {
                        sb.append("  ").append(formatMapValue((Map<String, Object>) item)).append("\n");
                    } else if (item instanceof String) {
                        sb.append("  ").append(item).append("\n");
                    } else if (item instanceof List) {
                        sb.append("  - ").append(formatListValue((List<?>) item)).append("\n");
                    }
                }
            }
            sb.append("\n\n");
        }
        return sb.toString();
    }

    /**
     * 格式化 Map 值为字符串
     */
    private String formatMapValue(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sb.append(entry.getKey()).append(": ");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append(value.toString());
            } else if (value instanceof Number) {
                sb.append(value.toString());
            } else if (value instanceof Boolean) {
                sb.append(value.toString());
            } else if (value instanceof List) {
                sb.append(formatListValue((List<?>) value));
            } else if (value instanceof Map) {
                sb.append(formatMapValue((Map<String, Object>) value));
            } else {
                sb.append(String.valueOf(value));
            }
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 格式化 List 值为字符串
     */
    private String formatListValue(List<?> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (Object item : list) {
            if (item instanceof String) {
                sb.append(item.toString());
            } else if (item instanceof Number) {
                sb.append(item.toString());
            } else if (item instanceof Boolean) {
                sb.append(item.toString());
            } else if (item instanceof Map) {
                sb.append(formatMapValue((Map<String, Object>) item));
            } else if (item instanceof List) {
                sb.append(formatListValue((List<?>) item));
            } else {
                sb.append(String.valueOf(item));
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
