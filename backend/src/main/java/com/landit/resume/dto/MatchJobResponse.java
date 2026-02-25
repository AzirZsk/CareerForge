package com.landit.resume.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 岗位JD匹配响应DTO
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchJobResponse {

    /**
     * 匹配分数 (0-100)
     */
    private Integer matchScore;

    /**
     * 关键词分析
     */
    private KeywordAnalysis keywordAnalysis;

    /**
     * 要求检查
     */
    private RequirementCheck requirementCheck;

    /**
     * 风险分析
     */
    private RiskAnalysis riskAnalysis;

    /**
     * 优化建议
     */
    private List<JobSuggestion> suggestions;

    /**
     * 总体建议
     */
    private String overallAdvice;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeywordAnalysis {
        private List<String> matched;
        private List<String> missing;
        private List<String> partialMatch;
        private String matchRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequirementCheck {
        private Map<String, RequirementItem> mustHave;
        private Map<String, RequirementItem> niceToHave;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequirementItem {
        private String required;
        private String actual;
        private String status; // pass/warn/fail
        private String detail;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskAnalysis {
        private List<String> redFlags;
        private List<String> warnings;
        private String passProbability; // 高/中/低
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JobSuggestion {
        private String priority;
        private String category;
        private String title;
        private String action;
        private String reason;
        private String position;
    }

}
