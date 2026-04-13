package com.careerforge.interview.dto.interviewcenter;

import lombok.Data;

/**
 * 复盘分析请求 DTO
 * 用于接收用户输入的面试过程文本
 *
 * @author Azir
 */
@Data
public class ReviewAnalysisRequest {

    /**
     * 面试过程文字记录
     * 用户输入或音频转录的面试过程中的问题、回答、面试官反馈等内容
     */
    private String transcript;

}
