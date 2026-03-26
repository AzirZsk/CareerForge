package com.landit.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.landit.chat.entity.ChatMessage;
import com.landit.chat.mapper.ChatMessageMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * AI聊天消息服务
 * 管理聊天消息的持久化存储
 * 支持两种模式：简历对话（resumeId）和通用聊天（sessionId）
 *
 * @author Azir
 */
@Service
public class ChatMessageService extends ServiceImpl<ChatMessageMapper, ChatMessage> {

    /**
     * 保存消息（简历模式）
     *
     * @param sessionId 会话ID（通用模式为UUID，简历模式为resumeId）
     * @param resumeId  简历ID（可选，通用模式为空）
     * @param role      角色（user / assistant）
     * @param content   消息内容
     */
    public void saveMessage(String sessionId, String resumeId, String role, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setResumeId(resumeId);
        msg.setRole(role);
        msg.setContent(content);
        save(msg);
    }

    /**
     * 获取历史消息（按时间升序，最近 N 条）
     * 使用MyBatis-Plus分页插件，避免手动拼接SQL
     *
     * @param sessionId 会话ID
     * @param limit     限制条数
     * @return 消息列表
     */
    public List<ChatMessage> getHistory(String sessionId, int limit) {
        // 先查询总数
        long total = count(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId));

        if (total == 0) {
            return Collections.emptyList();
        }

        // 计算偏移量：获取最近的N条（按时间升序）
        int offset = Math.max(0, (int) total - limit);

        // 使用分页查询获取数据
        Page<ChatMessage> page = new Page<>(offset / limit + 1, limit);
        Page<ChatMessage> result = page(page, new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreatedAt));

        return result.getRecords();
    }

    /**
     * 清空指定会话的聊天历史
     *
     * @param sessionId 会话ID
     */
    public void clearHistory(String sessionId) {
        remove(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId));
    }

    /**
     * 判断是否是该会话的首次对话
     *
     * @param sessionId 会话ID
     * @return true 表示首次对话，false 表示已有历史消息
     */
    public boolean isFirstMessage(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return true;
        }
        return count(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)) == 0;
    }
}
