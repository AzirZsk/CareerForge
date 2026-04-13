package com.landit.interview.dto;

import com.landit.common.annotation.SchemaField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 生成面试准备事项结果包装类
 * 用于约束 AI 返回 JSON 格式
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratePreparationResult {

    /**
     * 面试准备事项列表
     */
    @SchemaField(value = "面试准备事项列表（5-8项）", required = true)
    private List<PreparationItemResult> items;

}
