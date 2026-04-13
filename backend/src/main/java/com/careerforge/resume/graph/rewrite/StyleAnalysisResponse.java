package com.careerforge.resume.graph.rewrite;

import com.careerforge.common.annotation.SchemaField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 风格分析响应DTO
 * AI 分析参考简历后返回的风格特征
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StyleAnalysisResponse {

    /**
     * 写作语气特征描述
     */
    @SchemaField("写作语气特征描述，如：专业正式、亲切自然等")
    private String toneDescription;

    /**
     * 人称视角和语态模式
     */
    @SchemaField("人称视角和语态模式，如：主动语态为主(80%)，省略主语，无我字")
    private String voicePattern;

    /**
     * 句式结构特征
     */
    @SchemaField("句式结构特征，如：短句为主(平均12字)，祈使句+名词短语混合")
    private String sentenceStructure;

    /**
     * 描述方法论
     */
    @SchemaField("描述方法论，如：STAR法则，以动作开头以结果收尾")
    private String descriptionApproach;

    /**
     * 量化数据使用密度
     */
    @SchemaField("量化数据使用密度，如：高-70%的要点包含具体数字")
    private String quantificationDensity;

    /**
     * 动词使用风格
     */
    @SchemaField("动词使用风格，如：强动词主导(主导/构建/设计)，避免负责/参与")
    private String verbStyle;

    /**
     * 内容组织模式
     */
    @SchemaField("内容组织模式，如：职位-上下文-核心成就三段式")
    private String contentOrganization;

    /**
     * 区块排列顺序
     */
    @SchemaField("区块排列顺序，如工作经历、项目经验、教育背景、专业技能")
    private List<String> sectionOrdering;

    /**
     * 风格要点总结（核心风格规则，给改写节点使用）
     */
    @SchemaField("核心风格规则摘要，5-8条可执行的改写规则")
    private List<String> styleRules;

    /**
     * 参考简历中的风格示例
     */
    @SchemaField("从参考简历中提取的典型表达示例")
    private List<StyleExample> styleExamples;

    /**
     * 风格示例
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StyleExample {

        /**
         * 示例来源区块类型
         */
        @SchemaField("示例来源区块类型，如work/project/skills")
        private String sectionType;

        /**
         * 参考简历中的原始表达
         */
        @SchemaField("参考简历中的典型表达片段")
        private String referenceText;

        /**
         * 风格要点说明
         */
        @SchemaField("该示例体现的风格要点")
        private String stylePoint;
    }

}
