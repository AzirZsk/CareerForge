<template>
  <div class="transcript-display" ref="containerRef">
    <div
      v-for="(message, index) in messages"
      :key="index"
      :class="['message-item', message.role]"
    >
      <div class="message-header">
        <span class="role-icon">{{ getRoleIcon(message.role) }}</span>
        <span class="role-name">{{ getRoleName(message.role) }}</span>
        <span v-if="message.timestamp" class="timestamp">
          {{ formatTime(message.timestamp) }}
        </span>
      </div>
      <div class="message-content">
        {{ message.text }}
        <span v-if="!message.isFinal" class="typing-indicator">...</span>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="messages.length === 0" class="empty-state">
      <span class="empty-icon">💬</span>
      <p>对话将在这里显示...</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import type { TranscriptMessage } from '@/types/interview-voice'

// Props
interface Props {
  messages: TranscriptMessage[]
  autoScroll?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  messages: () => [],
  autoScroll: true
})

const containerRef = ref<HTMLElement | null>(null)

// 角色图标
function getRoleIcon(role: string): string {
  const icons: Record<string, string> = {
    interviewer: '👔',
    candidate: '👤',
    assistant: '🤖'
  }
  return icons[role] || '💬'
}

// 角色名称
function getRoleName(role: string): string {
  const names: Record<string, string> = {
    interviewer: '面试官',
    candidate: '候选人',
    assistant: 'AI 助手'
  }
  return names[role] || role
}

// 格式化时间
function formatTime(timestamp: number): string {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 自动滚动到底部
watch(
  () => props.messages.length,
  async () => {
    if (props.autoScroll) {
      await nextTick()
      if (containerRef.value) {
        containerRef.value.scrollTop = containerRef.value.scrollHeight
      }
    }
  }
)
</script>

<style scoped lang="scss">
.transcript-display {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  height: 100%;
  overflow-y: auto;
  background: var(--bg-primary);
  border-radius: 12px;
}

.message-item {
  padding: 12px;
  border-radius: 8px;
  background: var(--bg-secondary);

  &.interviewer {
    background: var(--bg-secondary);
    border-left: 3px solid var(--primary-color);
  }

  &.candidate {
    background: var(--bg-tertiary);
    border-left: 3px solid var(--success-color);
  }

  &.assistant {
    background: var(--bg-info);
    border-left: 3px solid var(--info-color);
  }

  .message-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
    font-size: 12px;
    color: var(--text-secondary);

    .role-icon {
      font-size: 16px;
    }

    .role-name {
      font-weight: 500;
    }

    .timestamp {
      margin-left: auto;
      opacity: 0.7;
    }
  }

  .message-content {
    line-height: 1.6;
    color: var(--text-primary);

    .typing-indicator {
      animation: blink 1s infinite;
    }
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-secondary);

  .empty-icon {
    font-size: 48px;
    margin-bottom: 16px;
    opacity: 0.5;
  }

  p {
    font-size: 14px;
    opacity: 0.7;
  }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
</style>
