package com.careerforge.interview.voice.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * WebSocket 消息类型枚举
 *
 * @author Azir
 */
@Getter
@RequiredArgsConstructor
public enum MessageType {

    /**
     * 音频消息
     */
    AUDIO("audio", "音频消息"),

    /**
     * 控制消息
     */
    CONTROL("control", "控制消息"),

    /**
     * 心跳消息
     */
    PING("ping", "心跳消息");

    private final String code;
    private final String description;

    /**
     * 根据 code 获取枚举
     */
    public static MessageType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (MessageType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
