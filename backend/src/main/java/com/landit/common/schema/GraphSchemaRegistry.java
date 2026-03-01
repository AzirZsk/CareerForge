package com.landit.common.schema;

import com.landit.common.annotation.SchemaField;
import com.landit.common.util.SchemaGenerator;
import com.landit.resume.dto.DiagnoseResumeResponse;
import com.landit.resume.dto.OptimizeSectionResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Graph 节点 JSON Schema 注册表
 * 为 Graph 工作流中的 AI 调用节点提供 JSON Schema 支持
 *
 * @author Azir
 */
@Component
public class GraphSchemaRegistry {

    /**
     * 构建简历诊断响应 Schema
     * 用于 DiagnoseResumeNode 和 DiagnosePreciseResumeNode
     *
     * @return 诊断响应的 JSON Schema
     */
    public Map<String, Object> buildDiagnosisSchema() {
        return SchemaGenerator.fromClass(DiagnoseResumeResponse.class);
    }

    /**
     * 构建模块优化响应 Schema
     * 用于 OptimizeSectionNode
     *
     * @return 模块优化响应的 JSON Schema
     */
    public Map<String, Object> buildOptimizeSectionSchema() {
        return SchemaGenerator.fromClass(OptimizeSectionResponse.class);
    }

    /**
     * 构建优化建议生成响应 Schema
     * 用于 GenerateSuggestionsNode
     *
     * @return 优化建议响应的 JSON Schema
     */
    public Map<String, Object> buildSuggestionsSchema() {
        return SchemaGenerator.fromClass(SuggestionsResponse.class);
    }

    /**
     * 优化建议响应内部类
     * 用于生成建议节点的 JSON Schema
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuggestionsResponse {
        @SchemaField(value = "优化建议列表")
        private List<DiagnoseResumeResponse.Suggestion> suggestions;

        @SchemaField(value = "快速改进建议列表")
        private List<String> quickWins;

        @SchemaField(value = "预估提升分数(0-100)")
        private Integer estimatedImprovement;
    }
}
