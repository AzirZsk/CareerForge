package com.landit.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.landit.chat.entity.ChatMessage;
import com.landit.chat.mapper.ChatMessageMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI聊天消息服务
 * 管理聊天消息的持久化存储
 *
 * @author Azir
 */
@Service
public class ChatMessageService extends ServiceImpl<ChatMessageMapper, ChatMessage> {

    /**
     * 保存消息
     *
     * @param resumeId 简历ID
     * @param role     角色（user / assistant）
     * @param content  消息内容
     */
    public void saveMessage(String resumeId, String role, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setResumeId(resumeId);
        msg.setRole(role);
        msg.setContent(content);
        save(msg);
    }

    /**
     * 获取历史消息（按时间升序，最近 N 条）
     *
     * @param resumeId 简历ID
     * @param limit    限制条数
     * @return 消息列表
     */
    public List<ChatMessage> getHistory(String resumeId, int limit) {
        return list(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getResumeId, resumeId)
                .orderByAsc(ChatMessage::getCreatedAt)
                .last("LIMIT " + limit));
    }

    /**
     * 清空指定简历的聊天历史
     *
     * @param resumeId 简历ID
     */
    public void clearHistory(String resumeId) {
        remove(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getResumeId, resumeId));
    }

    /**
     * 判断是否是该简历的首次对话
     *
     * @param resumeId 简历ID
     * @return true 表示首次对话，false 表示已有历史消息
     */
    public boolean isFirstMessage(String resumeId) {
        if (resumeId == null || resumeId.isEmpty()) {
            return true;
        }
        return count(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getResumeId, resumeId)) == 0;
    }
}
