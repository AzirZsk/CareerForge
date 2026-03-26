package com.landit.interview.voice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ASR（语音识别）结果
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ASRResult {

    /** 识别文本 */
    private String text;

    /** 是否最终结果 */
    private Boolean isFinal;

    /** 置信度 0-1 */
    private Double confidence;

    /** 开始时间（毫秒） */
    private Long startTime;

    /** 结束时间（毫秒） */
    private Long endTime;

    /** 创建中间结果 */
    public static ASRResult partial(String text) {
        return new ASRResult(text, false, null, null, null);
    }

    /** 创建最终结果 */
    public static ASRResult finalResult(String text) {
        return new ASRResult(text, true, null, null, null);
    }

    /** 创建带置信度的最终结果 */
    public static ASRResult finalResult(String text, Double confidence) {
        return new ASRResult(text, true, confidence, null, null);
    }

    /** 创建完整结果 */
    public static ASRResult finalResult(String text, Double confidence, Long startTime, Long endTime) {
        return new ASRResult(text, true, confidence, startTime, endTime);
    }
}
