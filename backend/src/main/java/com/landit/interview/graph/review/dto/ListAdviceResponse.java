package com.landit.interview.graph.review.dto;

import com.landit.common.annotation.SchemaField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 改进建议列表响应 DTO
 * 用于包装 AI 返回的建议列表（JSON Schema 约束需要对象包装）
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListAdviceResponse {

    /**
     * 改进建议列表
     */
    @SchemaField(value = "改进建议列表")
    private List<AdviceItem> adviceList;
}
