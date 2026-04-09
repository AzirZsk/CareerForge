package com.landit.interview.voice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 预生成问题
 * 缓存预生成的面试问题文本，供模拟面试零延迟推送
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreGeneratedQuestion {

    /**
     * 问题序号（0 = 开场白+第1题）
     */
    private Integer questionIndex;

    /**
     * 问题文本
     */
    private String text;

    /**
     * 是否已使用
     */
    private boolean used = false;
}
