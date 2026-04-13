package com.careerforge.interview.voice.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * WAV 文件头工具类
 * 用于创建 PCM 数据的 WAV 文件头
 *
 * @author Azir
 */
public final class WavHeaderUtils {
    // 音频参数常量
    public static final int SAMPLE_RATE = 16000;
    public static final int BITS_PER_SAMPLE = 16;
    public static final int NUM_CHANNELS = 1;
    public static final int BYTE_RATE = SAMPLE_RATE * NUM_CHANNELS * BITS_PER_SAMPLE / 8;
    public static final int BLOCK_ALIGN = NUM_CHANNELS * BITS_PER_SAMPLE / 8;

    private WavHeaderUtils() {
        // 工具类禁止实例化
    }

    /**
     * 转换时间戳
     *
     * @param startTime 开始时间
     * @return 时间戳（毫秒）
     */
    public static long convertToTimestamp(LocalDateTime startTime) {
        if (startTime == null) {
            return 0L;
        }
        return startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 为 PCM 数据添加 WAV 文件头
     *
     * @param pcmData 原始 PCM 数据
     * @return 带 WAV 头的音频数据
     */
    public static byte[] createWavWithHeader(byte[] pcmData) {
        int dataSize = pcmData.length;
        int fileSize = 36 + dataSize;
        ByteBuffer buffer = ByteBuffer.allocate(44 + dataSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        // RIFF header
        buffer.put("RIFF".getBytes());
        buffer.putInt(fileSize);
        buffer.put("WAVE".getBytes());
        // fmt chunk
        buffer.put("fmt ".getBytes());
        buffer.putInt(16);
        buffer.putShort((short) 1);
        buffer.putShort((short) NUM_CHANNELS);
        buffer.putInt(SAMPLE_RATE);
        buffer.putInt(BYTE_RATE);
        buffer.putShort((short) BLOCK_ALIGN);
        buffer.putShort((short) BITS_PER_SAMPLE);
        // data chunk
        buffer.put("data".getBytes());
        buffer.putInt(dataSize);
        buffer.put(pcmData);
        return buffer.array();
    }
}
