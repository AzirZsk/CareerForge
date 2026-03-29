package com.landit.interview.voice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TTS 合成块
 *
 * @author Azir
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TTSChunk {

    /** 对应的文本片段 */
    private String text;

    /** 音频数据 */
    private byte[] audio;

    /** 是否最后一个块 */
    private Boolean isFinal;

    /** 创建普通块 */
    public static TTSChunk of(String text, byte[] audio) {
        return new TTSChunk(text, audio, false);
    }

    /** 创建最终块 */
    public static TTSChunk finalChunk(String text, byte[] audio) {
        return new TTSChunk(text, audio, true);
    }
}
