package com.landit.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 语音服务配置属性
 * 支持 ASR（语音识别）和 TTS（语音合成）多服务商配置
 *
 * @author Azir
 */
@Data
@Component
@ConfigurationProperties(prefix = "landit.voice")
public class VoiceProperties {

    /**
     * 语音服务提供商：aliyun
     * 启动时确定，不支持运行时切换
     */
    private String provider = "aliyun";

    /**
     * 阿里云语音服务配置
     */
    private AliyunConfig aliyun = new AliyunConfig();

    /**
     * 阿里云语音服务配置
     */
    @Data
    public static class AliyunConfig {
        /**
         * 阿里云 API Key
         */
        private String apiKey;

        /**
         * ASR 配置
         */
        private ASRConfig asr = new ASRConfig();

        /**
         * TTS 配置
         */
        private TTSConfig tts = new TTSConfig();

        /**
         * 音色配置
         */
        private VoicesConfig voices = new VoicesConfig();

        /**
         * 离线 ASR 配置（录音文件识别）
         */
        private FileASRConfig fileAsr = new FileASRConfig();

        @Data
        public static class ASRConfig {
            /**
             * ASR 模型（快照版，支持中英日多语种）
             */
            private String model = "fun-asr-realtime-2026-02-28";

            /**
             * 音频格式：pcm, wav, mp3
             */
            private String format = "pcm";

            /**
             * 采样率
             */
            private Integer sampleRate = 16000;

            /**
             * 是否启用标点预测
             */
            private Boolean enablePunctuation = true;

            /**
             * 是否启用逆文本正则化（数字转阿拉伯）
             */
            private Boolean enableItn = true;

            /**
             * 语言：zh, en
             */
            private String language = "zh";

            /**
             * 是否启用语音活动检测
             */
            private Boolean enableVad = true;
        }

        @Data
        public static class TTSConfig {
            /**
             * TTS 模型
             */
            private String model = "cosyvoice-v2";

            /**
             * 输出格式：pcm, wav, mp3
             */
            private String format = "wav";

            /**
             * 采样率
             */
            private Integer sampleRate = 16000;
        }

        @Data
        public static class VoicesConfig {
            /**
             * 面试官音色
             */
            private String interviewer = "longxiaochun_v2";

            /**
             * AI助手音色（更亲切）
             */
            private String assistant = "zhimiao_emo_v2";
        }

        /**
         * 离线 ASR 配置（录音文件识别）
         * 用于复盘场景上传音频文件转文字
         */
        @Data
        public static class FileASRConfig {
            /**
             * 离线 ASR 模型
             * Paraformer 录音文件识别模型
             */
            private String model = "paraformer-v2";

            /**
             * 语言：zh, en, ja, ko 等
             */
            private String language = "zh";

            /**
             * 最大文件大小（MB）
             */
            private Integer maxFileSizeMb = 50;

            /**
             * 轮询间隔（毫秒）
             */
            private Long pollingIntervalMs = 2000L;
        }
    }

}
