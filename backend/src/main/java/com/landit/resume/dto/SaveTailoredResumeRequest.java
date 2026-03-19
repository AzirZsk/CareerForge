package com.landit.resume.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 保存定制简历请求DTO
 * 用于将定制后的简历保存为新的派生简历
 *
 * @author Azir
 */
@Data
public class SaveTailoredResumeRequest {

    @NotBlank(message = "目标岗位不能为空")
    private String targetPosition;

    private String resumeName;

    @NotBlank(message = "职位描述不能为空")
    private String jobDescription;

    @NotEmpty(message = "定制内容不能为空")
    @Valid
    private List<SectionDataItem> afterSection;

    /**
     * 整体提升分数
     */
    private Integer improvementScore;

    /**
     * 岗位匹配分数
     */
    private Integer matchScore;

    /**
     * 区块相关性评分
     */
    private Map<String, Integer> sectionRelevanceScores;

    /**
     * 四大维度评分
     */
    private DimensionScores dimensionScores;

    /**
     * 区块数据项
     */
    @Data
    public static class SectionDataItem {
        private String id;
        private String type;
        private String title;
        @NotBlank(message = "区块内容不能为空")
        private String content;
        /**
         * 区块评分
         */
        private Integer score;
    }

    /**
     * 四大维度评分
     */
    @Data
    public static class DimensionScores {
        private Integer content;
        private Integer structure;
        private Integer matching;
        private Integer competitiveness;
    }
}
