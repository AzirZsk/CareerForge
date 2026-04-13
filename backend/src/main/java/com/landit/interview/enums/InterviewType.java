package com.landit.interview.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 面试类型枚举
 *
 * @author Azir
 */
@Getter
@RequiredArgsConstructor
public enum InterviewType {

    ONSITE("onsite", "现场面试"),
    ONLINE("online", "线上面试");

    private final String code;
    private final String description;

}
