package com.landit.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.landit.common.entity.BaseEntity;
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
@TableName(value = "t_conversation", autoResultMap = true)
public class Conversation extends BaseEntity {

    private String sessionId;

    private String role;

    private String content;

    private LocalDateTime timestamp;

    private Integer score;

    private String feedback;

}
