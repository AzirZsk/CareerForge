<!--=====================================================
  单条聊天消息组件
  支持用户/AI/系统消息，支持打字机效果
  @author Azir
=====================================================-->

<template>
  <div
    class="message-item"
    :class="{
      'message-user': message.role === 'user',
      'message-assistant': message.role === 'assistant',
      'message-system': message.role === 'system',
      'message-streaming': message.isStreaming
    }"
  >
    <!-- 用户头像 -->
    <div v-if="message.role !== 'system'" class="message-avatar">
      <template v-if="message.role === 'user'">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
          <circle cx="12" cy="7" r="4"></circle>
        </svg>
      </template>
      <template v-else>
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 2a2 2 0 0 1 2 2c0 .74-.4 1.39-1 1.73V7h1a7 7 0 0 1 7 7h1a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1h-1v1a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-1H2a1 1 0 0 1-1-1v-3a1 1 0 0 1 1-1h1a7 7 0 0 1 7-7h1V5.73c-.6-.34-1-.99-1-1.73a2 2 0 0 1 2-2z"/>
        </svg>
      </template>
    </div>

    <!-- 消息内容 -->
    <div class="message-content">
      <!-- 图片预览 -->
      <div v-if="message.imageUrl" class="message-image">
        <img :src="message.imageUrl" alt="上传的图片" />
      </div>

      <!-- 文本内容 -->
      <div class="message-text" v-html="formattedContent"></div>

      <!-- 流式输出光标 -->
      <span v-if="message.isStreaming" class="streaming-cursor"></span>

      <!-- 时间戳 -->
      <div class="message-time">{{ formatTime(message.timestamp) }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ChatMessage } from '@/types/ai-chat'
import { useMarkdown } from '@/composables/useMarkdown'

interface Props {
  message: ChatMessage
}

const props = defineProps<Props>()
const { renderMarkdown } = useMarkdown()

const formattedContent = computed(() => renderMarkdown(props.message.content))

function formatTime(timestamp: number): string {
  const date = new Date(timestamp)
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${hours}:${minutes}`
}
</script>

<style lang="scss" scoped>
.message-item {
  display: flex;
  gap: $spacing-sm;
  max-width: 85%;

  &.message-user {
    align-self: flex-end;
    flex-direction: row-reverse;

    .message-avatar {
      background: $color-accent;
    }

    .message-content {
      background: rgba($color-accent, 0.2);
      border: 1px solid rgba($color-accent, 0.3);
    }
  }

  &.message-assistant {
    align-self: flex-start;

    .message-avatar {
      background: $color-bg-tertiary;
      border: 1px solid rgba(255, 255, 255, 0.1);
    }

    .message-content {
      background: $color-bg-tertiary;
      border: 1px solid rgba(255, 255, 255, 0.06);
    }
  }

  &.message-system {
    align-self: center;
    max-width: 100%;

    .message-content {
      background: rgba($color-info, 0.1);
      border: 1px solid rgba($color-info, 0.2);
      text-align: center;
      font-size: $text-sm;
      color: $color-text-secondary;
    }
  }

  &.message-streaming {
    .message-content {
      border-color: rgba($color-accent, 0.3);
    }
  }
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: $radius-full;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: $color-text-primary;
}

.message-content {
  padding: $spacing-md $spacing-md $spacing-lg $spacing-md;
  border-radius: $radius-md;
  position: relative;

  :deep(p) {
    margin: 0 0 $spacing-sm 0;

    &:last-child {
      margin-bottom: 0;
    }
  }

  :deep(ul), :deep(ol) {
    margin: $spacing-sm 0;
    padding-left: $spacing-lg;
  }

  :deep(code) {
    background: rgba(0, 0, 0, 0.2);
    padding: 2px 6px;
    border-radius: $radius-sm;
    font-size: $text-sm;
  }

  :deep(pre) {
    background: rgba(0, 0, 0, 0.2);
    padding: $spacing-md;
    border-radius: $radius-sm;
    overflow-x: auto;
    margin: $spacing-sm 0;

    code {
      background: transparent;
      padding: 0;
    }
  }
}

.message-image {
  margin-bottom: $spacing-sm;

  img {
    max-width: 100%;
    max-height: 200px;
    border-radius: $radius-sm;
    object-fit: cover;
  }
}

.message-text {
  line-height: $leading-relaxed;
  word-break: break-word;
}

.streaming-cursor {
  display: inline-block;
  width: 2px;
  height: 1em;
  background: $color-accent;
  margin-left: 2px;
  animation: blink 1s infinite;
  vertical-align: text-bottom;
}

@keyframes blink {
  0%, 50% {
    opacity: 1;
  }
  51%, 100% {
    opacity: 0;
  }
}

.message-time {
  position: absolute;
  right: $spacing-sm;
  bottom: $spacing-xs;
  font-size: 10px;
  color: $color-text-tertiary;
  opacity: 0.7;
}
</style>
