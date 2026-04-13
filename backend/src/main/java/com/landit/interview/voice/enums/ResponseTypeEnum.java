package com.landit.interview.voice.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * WebSocket 响应消息类型枚举（服务端 -> 客户端）
 *
 * @author Azir
 */
@Getter
@RequiredArgsConstructor
public enum ResponseTypeEnum {

    /**
     * 转录结果
     */
    TRANSCRIPT("transcript", "转录结果"),

    /**
     * 音频数据
     */
    AUDIO("audio", "音频数据"),

    /**
     * 状态更新
     */
    STATE("state", "状态更新"),

    /**
     * 错误信息
     */
    ERROR("error", "错误信息"),

    /**
     * 心跳响应
     */
    PONG("pong", "心跳响应"),

    /**
     * 准备就绪
     */
    READY("ready", "准备就绪");

    private final String code;
    private final String description;
}
