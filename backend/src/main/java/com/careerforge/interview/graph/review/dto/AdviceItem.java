package com.careerforge.interview.graph.review.dto;

import com.careerforge.common.annotation.SchemaField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 改进建议项 DTO
 * AI 生成的单条改进建议
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdviceItem {

    /**
     * 建议类别
     * 可选值：技能提升/面试技巧/项目经验/行为面试/后续行动
     */
    @SchemaField(value = "建议类别")
    private String category;

    /**
     * 建议标题
     */
    @SchemaField(value = "建议标题")
    private String title;

    /**
     * 详细描述
     */
    @SchemaField(value = "详细描述")
    private String description;

    /**
     * 优先级(高/中/低)
     */
    @SchemaField(value = "优先级(高/中/低)")
    private String priority;

    /**
     * 具体行动项列表
     */
    @SchemaField(value = "具体行动项列表")
    private List<String> actionItems;
}
