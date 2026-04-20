package com.careerforge.interview.voice.dto;

import com.careerforge.common.annotation.SchemaField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 追问判断响应 DTO
 * LLM 单次调用同时返回"是否需要追问"的判断和追问内容
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowUpJudgeResponse {

    /**
     * 是否需要追问
     */
    @SchemaField(value = "是否需要追问", required = true)
    private boolean needFollowUp;

    /**
     * 追问问题文本（needFollowUp为true时必填，30字以内，口语化）
     */
    @SchemaField(value = "追问问题文本（needFollowUp为true时必填，30字以内，口语化表达）")
    private String followUpQuestion;

    /**
     * 判断理由（用于日志记录）
     */
    @SchemaField(value = "判断理由，简述为什么需要/不需要追问")
    private String reason;
}
