package com.landit.interview.entity;

import com.landit.common.entity.BaseEntity;
import com.landit.common.enums.ConversationRole;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 面试对话记录实体类
 *
 * @author Azir
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Conversation extends BaseEntity {

    private Long sessionId;

    private ConversationRole role;

    private String content;

    private LocalDateTime timestamp;

    private Integer score;

    private String feedback;

}
