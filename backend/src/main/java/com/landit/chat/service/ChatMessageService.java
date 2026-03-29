package com.landit.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.chat.dto.ChatMessageVO;
import com.landit.chat.dto.SectionChange;
import com.landit.chat.entity.ChatMessage;
import com.landit.chat.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * AI聊天消息服务
 * 管理聊天消息的持久化存储
 * 支持两种模式：简历对话（resumeId）和通用聊天（sessionId）
 *
 * @author Azir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService extends ServiceImpl<ChatMessageMapper, ChatMessage> {

    private final ObjectMapper objectMapper;

    /**
     * 有效操作状态集合
     */
    private static final Set<String> VALID_ACTION_STATUSES = Set.of("pending", "applied", "failed");

    /**
     * 保存消息（简历模式）
     *
     * @param sessionId 会话ID（通用模式为UUID，简历模式为resumeId）
     * @param resumeId  简历ID（可选，通用模式为空）
     * @param role      角色（user / assistant）
     * @param content   消息内容
     */
    public void saveMessage(String sessionId, String resumeId, String role, String content) {
        saveMessage(sessionId, resumeId, role, content, null);
    }

    /**
     * 保存消息（带操作卡片）
     *
     * @param sessionId 会话ID
     * @param resumeId  简历ID（可选）
     * @param role      角色
     * @param content   消息内容
     * @param actions   操作卡片列表（可选）
     */
    public void saveMessage(String sessionId, String resumeId, String role,
                            String content, List<SectionChange> actions) {
        saveMessage(sessionId, resumeId, role, content, actions, null);
    }

    /**
     * 保存消息（带操作卡片和内容分片）
     *
     * @param sessionId 会话ID
     * @param resumeId  简历ID（可选）
     * @param role      角色
     * @param content   消息内容
     * @param actions   操作卡片列表（可选）
     * @param segments  内容分片列表（可选，记录文字和操作卡片的穿插顺序）
     */
    public void saveMessage(String sessionId, String resumeId, String role,
                            String content, List<SectionChange> actions,
                            List<ChatMessageVO.ContentSegment> segments) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setResumeId(resumeId);
        msg.setRole(role);
        msg.setContent(content);
        // 序列化操作卡片
        if (actions != null && !actions.isEmpty()) {
            try {
                msg.setActions(objectMapper.writeValueAsString(actions));
                msg.setActionStatus("pending");
            } catch (JsonProcessingException e) {
                log.error("[ChatMessage] 序列化 actions 失败", e);
            }
        }
        // 序列化内容分片
        if (segments != null && !segments.isEmpty()) {
            try {
                msg.setSegments(objectMapper.writeValueAsString(segments));
            } catch (JsonProcessingException e) {
                log.error("[ChatMessage] 序列化 segments 失败", e);
            }
        }
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
     * 获取历史消息（VO格式，包含反序列化的操作卡片）
     *
     * @param sessionId 会话ID
     * @param limit     限制条数
     * @return VO消息列表
     */
    public List<ChatMessageVO> getHistoryVO(String sessionId, int limit) {
        List<ChatMessage> messages = getHistory(sessionId, limit);
        return messages.stream().map(this::toVO).toList();
    }

    /**
     * 实体转VO（含 actions 反序列化）
     */
    private ChatMessageVO toVO(ChatMessage msg) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(msg.getId());
        vo.setRole(msg.getRole());
        vo.setContent(msg.getContent());
        vo.setCreatedAt(msg.getCreatedAt());
        vo.setActionStatus(msg.getActionStatus());
        // 反序列化 actions
        if (msg.getActions() != null && !msg.getActions().isEmpty()) {
            try {
                List<SectionChange> actions = objectMapper.readValue(
                        msg.getActions(),
                        new TypeReference<>() {}
                );
                vo.setActions(actions);
            } catch (JsonProcessingException e) {
                log.error("[ChatMessage] 反序列化 actions 失败", e);
            }
        }
        // 反序列化 segments
        if (msg.getSegments() != null && !msg.getSegments().isEmpty()) {
            try {
                List<ChatMessageVO.ContentSegment> segments = objectMapper.readValue(
                        msg.getSegments(),
                        new TypeReference<>() {}
                );
                vo.setSegments(segments);
            } catch (JsonProcessingException e) {
                log.error("[ChatMessage] 反序列化 segments 失败", e);
            }
        }
        return vo;
    }

    /**
     * 更新操作状态
     *
     * @param messageId 消息ID
     * @param status    新状态（pending / applied / failed）
     */
    public void updateActionStatus(String messageId, String status) {
        // 校验状态有效性
        if (status == null || !VALID_ACTION_STATUSES.contains(status.toLowerCase())) {
            throw new IllegalArgumentException("无效的操作状态: " + status);
        }
        update(new LambdaUpdateWrapper<ChatMessage>()
                .eq(ChatMessage::getId, messageId)
                .set(ChatMessage::getActionStatus, status.toLowerCase()));
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
