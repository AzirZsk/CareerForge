package com.landit.resume.dto;

import com.landit.common.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简历解析结果DTO
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeParseResult {

    /**
     * 姓名（兼容旧逻辑）
     */
    private String name;

    /**
     * 性别（兼容旧逻辑）
     */
    private Gender gender;

    /**
     * 原始 Markdown 文本
     */
    private String rawText;

    /**
     * 结构化简历数据
     */
    private ResumeStructuredData structuredData;

}
