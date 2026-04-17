package com.careerforge.interview.voice.service;

import com.careerforge.interview.voice.dto.ASRResult;

/**
 * ASR 识别结果回调接口
 * 替代 Flux 响应式流，采用简单直接的回调模式
 *
 * @author Azir
 */
public interface ASRListener {

    /**
     * 收到识别结果（中间结果 isFinal=false + 最终结果 isFinal=true）
     *
     * @param result 识别结果
     */
    void onResult(ASRResult result);

    /**
     * 识别过程发生错误
     *
     * @param e 错误信息
     */
    void onError(Exception e);

    /**
     * 识别完成，连接已关闭
     */
    void onComplete();
}
