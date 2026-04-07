package com.landit.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 音频转录任务结果
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioTranscribeResult {

    /**
     * 转录文本
     */
    private String transcriptText;

    /**
     * 音频时长（秒）
     */
    private Integer duration;

}
