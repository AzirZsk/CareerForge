<template>
  <div class="transcript-display" ref="containerRef">
    <div class="messages-container">
      <TransitionGroup name="message">
        <div
          v-for="message in messages"
          :key="message.id"
          :class="['message-item', `role-${message.role}`]"
        >
          <div class="message-header">
            <span class="role-icon">{{ getRoleIcon(message.role) }}</span>
            <span class="role-name">{{ getRoleName(message.role) }}</span>
            <span class="timestamp">{{ formatTime(message.timestamp) }}</span>
          </div>
          <div class="message-content">{{ message.content }}</div>
        </div>
      </TransitionGroup>

      <!-- 流式输入中的消息 -->
      <div v-if="isStreaming && streamingText" :class="['message-item', `role-${streamingRole}`, 'streaming']">
        <div class="message-header">
          <span class="role-icon">{{ getRoleIcon(streamingRole) }}</span>
          <span class="role-name">{{ getRoleName(streamingRole) }}</span>
          <span class="streaming-indicator">
            <span class="dot"></span>
            <span class="dot"></span>
            <span class="dot"></span>
          </span>
        </div>
        <div class="message-content">
          {{ streamingText }}
          <span class="cursor">|</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import type { ConversationMessage, ConversationRole } from '@/types/interview-voice'

// Props
interface Props {
  messages: ConversationMessage[]
  isStreaming?: boolean
  streamingText?: string
  streamingRole?: ConversationRole
  autoScroll?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  isStreaming: false,
  streamingText: '',
  streamingRole: 'interviewer',
  autoScroll: true
})

// Refs
const containerRef = ref<HTMLElement | null>(null)

// 角色图标映射
const roleIcons: Record<ConversationRole, string> = {
  interviewer: '👔',
  candidate: '👤',
  assistant: '🤖'
}

// 角色名称映射
const roleNames: Record<ConversationRole, string> = {
  interviewer: '面试官',
  candidate: '候选人',
  assistant: 'AI 助手'
}

// 获取角色图标
function getRoleIcon(role: ConversationRole): string {
  return roleIcons[role] || '💬'
}

// 获取角色名称
function getRoleName(role: ConversationRole): string {
  return roleNames[role] || '未知'
}

// 格式化时间
function formatTime(timestamp: number): string {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 滚动到底部
function scrollToBottom() {
  if (!props.autoScroll || !containerRef.value) return

  nextTick(() => {
    if (containerRef.value) {
      containerRef.value.scrollTop = containerRef.value.scrollHeight
    }
  })
}

// 监听消息变化，自动滚动
watch(
  () => props.messages.length,
  () => {
    scrollToBottom()
  }
)

// 监听流式文本变化，自动滚动
watch(
  () => props.streamingText,
  () => {
    scrollToBottom()
  }
)
</script>

<style lang="scss" scoped>
.transcript-display {
  height: 100%;
  overflow-y: auto;
  padding: $spacing-md;
  background: $color-bg-secondary;
  border-radius: $radius-md;

  .messages-container {
    display: flex;
    flex-direction: column;
    gap: $spacing-md;
  }
}

.message-item {
  padding: $spacing-md;
  border-radius: $radius-md;
  animation: fadeIn 0.3s ease;

  &.role-interviewer {
    background: linear-gradient(135deg, rgba($color-accent, 0.1), rgba($color-accent, 0.05));
    border-left: 3px solid $color-accent;
  }

  &.role-candidate {
    background: $color-bg-tertiary;
    border-left: 3px solid $color-info;
  }

  &.role-assistant {
    background: linear-gradient(135deg, rgba($color-success, 0.1), rgba($color-success, 0.05));
    border-left: 3px solid $color-success;
  }

  &.streaming {
    opacity: 0.9;
  }
}

.message-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-xs;

  .role-icon {
    font-size: 1.2rem;
  }

  .role-name {
    font-weight: 600;
    color: $color-text-primary;
    font-size: 0.875rem;
  }

  .timestamp {
    margin-left: auto;
    font-size: 0.75rem;
    color: $color-text-tertiary;
  }

  .streaming-indicator {
    display: flex;
    gap: 4px;
    margin-left: $spacing-sm;

    .dot {
      width: 6px;
      height: 6px;
      background: $color-accent;
      border-radius: 50%;
      animation: bounce 1.4s infinite ease-in-out both;

      &:nth-child(1) {
        animation-delay: -0.32s;
      }

      &:nth-child(2) {
        animation-delay: -0.16s;
      }

      &:nth-child(3) {
        animation-delay: 0s;
      }
    }
  }
}

.message-content {
  color: $color-text-secondary;
  font-size: 0.9375rem;
  line-height: 1.6;
  word-break: break-word;

  .cursor {
    animation: blink 1s infinite;
    color: $color-accent;
  }
}

// 动画
.message-enter-active {
  transition: all 0.3s ease;
}

.message-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes bounce {
  0%,
  80%,
  100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

@keyframes blink {
  0%,
  50% {
    opacity: 1;
  }
  51%,
  100% {
    opacity: 0;
  }
}
</style>
