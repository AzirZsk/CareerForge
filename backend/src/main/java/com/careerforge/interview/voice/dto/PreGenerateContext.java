package com.careerforge.interview.voice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 预生成上下文
 * 用于传递预生成问题所需的上下文信息
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreGenerateContext {

    /**
     * 面试职位
     */
    private String position;

    /**
     * JD 内容
     */
    private String jdContent;

    /**
     * 简历内容
     */
    private String resumeContent;

    /**
     * 面试官风格
     */
    private String interviewerStyle;

    /**
     * 总问题数
     */
    private Integer totalQuestions;

    /**
     * JD 分析内容（可选）
     */
    private String jdAnalysis;

}
