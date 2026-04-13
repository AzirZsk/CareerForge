package com.landit.resume.dto;

import com.landit.common.annotation.SchemaField;
import com.landit.resume.dto.ResumeStructuredData.BasicInfo;
import com.landit.resume.dto.ResumeStructuredData.EducationExperience;
import com.landit.resume.dto.ResumeStructuredData.WorkExperience;
import com.landit.resume.dto.ResumeStructuredData.ProjectExperience;
import com.landit.resume.dto.ResumeStructuredData.Skill;
import com.landit.resume.dto.ResumeStructuredData.Certificate;
import com.landit.resume.dto.ResumeStructuredData.OpenSourceContribution;
import com.landit.resume.dto.ResumeStructuredData.CustomSection;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 定制简历生成结果 DTO
 *
 * @author Azir
 */
@Data
public class TailorResumeResponse {

    /**
     * 基本信息（定制后）
     */
    @SchemaField(value = "基本信息（定制后，targetPosition改为目标职位，summary突出与JD相关的核心优势）")
    private BasicInfo basicInfo;

    /**
     * 教育经历（定制后）
     */
    @SchemaField(value = "教育经历（保持原样，不做精简）")
    private List<EducationExperience> education;

    /**
     * 工作经历（定制后，已强调相关性）
     */
    @SchemaField(value = "工作经历（全部保留，高相关的详细展开，低相关的精简为1行）")
    private List<WorkExperience> work;

    /**
     * 项目经历（定制后，已强调相关性）
     */
    @SchemaField(value = "项目经历（仅保留与JD相关的项目，删除无关项目）")
    private List<ProjectExperience> projects;

    /**
     * 技能（定制后，已调整顺序和描述）
     */
    @SchemaField(value = "技能（仅保留相关技能，按JD关键词排序，删除无关技能）")
    private List<Skill> skills;

    /**
     * 证书（定制后）
     */
    @SchemaField(value = "证书（仅保留与JD相关的证书，删除无关证书）")
    private List<Certificate> certificates;

    /**
     * 开源贡献（定制后）
     */
    @SchemaField(value = "开源贡献（仅保留与JD相关的）")
    private List<OpenSourceContribution> openSource;

    /**
     * 自定义区块（定制后）
     */
    @SchemaField(value = "自定义区块（仅保留与JD相关的）")
    private List<CustomSection> customSections;

    /**
     * 定制说明（描述做了哪些调整，包括删除和精简操作）
     */
    @SchemaField(value = "定制说明（3-5条，描述具体做了哪些删除和精简操作）", required = true)
    private List<String> tailorNotes;

    /**
     * 被删除的内容（记录删除了哪些项目、技能、证书等）
     */
    @SchemaField(value = "被删除的内容记录（必须记录所有被删除的项目、技能、证书等）", required = true)
    private RemovedItems removedItems;

    /**
     * 模块评分（各区块与JD的相关性评分）
     */
    @SchemaField(value = "各区块与JD的相关性评分（0-100）", required = true)
    private Map<String, Integer> sectionRelevanceScores;

    /**
     * 定制简历四大维度评分
     */
    @SchemaField(value = "四大维度评分（content/structure/matching/competitiveness）", required = true)
    private DimensionScores dimensionScores;

    /**
     * 四大维度评分
     */
    @Data
    public static class DimensionScores {
        /**
         * 内容质量评分 (0-100)
         */
        @SchemaField(value = "内容质量评分：评估定制后简历的内容丰富度、准确性、专业性 (0-100)", required = true)
        private Integer content;

        /**
         * 结构规范评分 (0-100)
         */
        @SchemaField(value = "结构规范评分：评估格式规范、逻辑清晰、层次分明程度 (0-100)", required = true)
        private Integer structure;

        /**
         * 岗位匹配评分 (0-100)
         */
        @SchemaField(value = "岗位匹配评分：评估与目标岗位的契合度 (0-100)", required = true)
        private Integer matching;

        /**
         * 竞争力评分 (0-100)
         */
        @SchemaField(value = "竞争力评分：评估在求职市场中的竞争优势 (0-100)", required = true)
        private Integer competitiveness;
    }

    /**
     * 被删除的内容记录
     */
    @Data
    public static class RemovedItems {
        /**
         * 被删除的项目名称列表
         */
        @SchemaField(value = "被删除的项目名称列表（如无删除则为空数组）")
        private List<String> projects;

        /**
         * 被删除的技能名称列表
         */
        @SchemaField(value = "被删除的技能名称列表（如无删除则为空数组）")
        private List<String> skills;

        /**
         * 被删除的证书名称列表
         */
        @SchemaField(value = "被删除的证书名称列表（如无删除则为空数组）")
        private List<String> certificates;

        /**
         * 被删除的其他内容（如开源贡献、自定义区块等）
         */
        @SchemaField(value = "被删除的其他内容，key为区块类型，value为名称列表（如无删除则为空对象）")
        private Map<String, List<String>> others;
    }

}
