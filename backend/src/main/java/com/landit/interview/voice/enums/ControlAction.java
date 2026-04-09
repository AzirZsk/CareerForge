package com.landit.interview.voice.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 语音面试控制动作枚举
 *
 * @author Azir
 */
@Getter
@RequiredArgsConstructor
public enum ControlAction {
    /**
     * 开始面试
     */
    START("start", "开始面试"),

    /**
     * 停止录音
     */
    STOP("stop", "停止录音"),

    /**
     * 结束面试
     */
    END("end", "结束面试"),

    /**
     * 冻结面试
     */
    FREEZE("freeze", "冻结面试"),

    /**
     * 恢复面试
     */
    RESUME("resume", "恢复面试");

    private final String code;
    private final String description;

    /**
     * 根据 code 获取枚举
     */
    public static ControlAction fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ControlAction action : values()) {
            if (action.getCode().equals(code)) {
                return action;
            }
        }
        return null;
    }
}
