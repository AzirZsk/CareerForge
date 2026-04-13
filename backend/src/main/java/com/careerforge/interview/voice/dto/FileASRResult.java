package com.careerforge.interview.voice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 离线语音识别结果
 * 用于录音文件识别场景（复盘上传音频）
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileASRResult {

    /**
     * 状态：processing（处理中）、completed（完成）、failed（失败）
     */
    private String status;

    /**
     * 转录文本（completed 时有值）
     */
    private String text;

    /**
     * 进度百分比 0-100
     */
    private Integer progress;

    /**
     * 状态消息
     */
    private String message;

    /**
     * 错误信息（failed 时有值）
     */
    private String errorMessage;

    // ===== 静态工厂方法 =====

    /**
     * 处理中状态
     */
    public static FileASRResult processing(String message, int progress) {
        return FileASRResult.builder()
                .status("processing")
                .message(message)
                .progress(progress)
                .build();
    }

    /**
     * 完成状态
     */
    public static FileASRResult completed(String text) {
        return FileASRResult.builder()
                .status("completed")
                .text(text)
                .progress(100)
                .message("转录完成")
                .build();
    }

    /**
     * 失败状态
     */
    public static FileASRResult failed(String errorMessage) {
        return FileASRResult.builder()
                .status("failed")
                .errorMessage(errorMessage)
                .message("转录失败")
                .build();
    }
}
