package com.careerforge.interview.voice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.careerforge.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 助手对话记录实体
 * 记录面试过程中的求助对话
 *
 * @author Azir
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("t_assistant_conversation")
public class AssistantConversation extends BaseEntity {

    /**
     * 关联的面试会话 ID
     */
    private String sessionId;

    /**
     * 冻结时的问题序号
     */
    private Integer freezeIndex;

    /**
     * 求助类型：GIVE_HINTS, EXPLAIN_CONCEPT, FREE_QUESTION
     */
    private String assistType;

    /**
     * 用户问题（自由提问时）
     */
    private String userQuestion;

    /**
     * 助手回复
     */
    private String assistantResponse;

    /**
     * 音频存储路径
     */
    private String audioUrl;

    /**
     * 音频时长（毫秒）
     */
    private Integer durationMs;
}
