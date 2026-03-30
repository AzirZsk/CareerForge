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
    <div
      v-if="message.role !== 'system'"
      class="message-avatar"
    >
      <template v-if="message.role === 'user'">
        <svg
          width="20"
          height="20"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
          <circle
            cx="12"
            cy="7"
            r="4"
          />
        </svg>
      </template>
      <template v-else>
        <AIIcon :size="20" />
      </template>
    </div>

    <!-- 消息主体 -->
    <div class="message-body">
      <!-- 消息内容 -->
      <div class="message-content">
        <!-- 多图预览 -->
        <div
          v-if="message.imageUrls && message.imageUrls.length > 0"
          class="message-images"
        >
          <div
            v-for="(url, index) in message.imageUrls"
            :key="index"
            class="message-image"
            @click="handleImageClick(url)"
          >
            <img
              :src="url"
              :alt="`图片 ${index + 1}`"
            >
            <div class="image-overlay">
              <svg
                width="24"
                height="24"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <circle
                  cx="11"
                  cy="11"
                  r="8"
                />
                <line
                  x1="21"
                  y1="21"
                  x2="16.65"
                  y2="16.65"
                />
                <line
                  x1="11"
                  y1="8"
                  x2="11"
                  y2="14"
                />
                <line
                  x1="8"
                  y1="11"
                  x2="14"
                  y2="11"
                />
              </svg>
            </div>
          </div>
        </div>

        <!-- 按 segments 顺序渲染（文字和操作卡片穿插） -->
        <template v-if="message.segments && message.segments.length > 0">
          <template v-for="(segment, segIndex) in message.segments" :key="segIndex">
            <!-- 文本分片 -->
            <div
              v-if="segment.type === 'text' && segment.content"
              class="message-text"
              :class="{ 'is-streaming': message.isStreaming && segIndex === message.segments!.length - 1 }"
              v-html="renderSegmentMarkdown(segment.content)"
            />
            <!-- 操作卡片分片：内联渲染 -->
            <div
              v-else-if="segment.type === 'action' && message.actions?.[segment.actionIndex]"
              class="inline-action-card"
            >
              <SectionChangeCard
                :change="message.actions[segment.actionIndex]"
                @apply="emit('apply-single-change', message.id, segment.actionIndex)"
                @ignore="emit('ignore-change', message.id, segment.actionIndex)"
              />
            </div>
          </template>
          <!-- 全部应用按钮（有多个 pending 时显示） -->
          <button
            v-if="hasMultiplePending"
            class="apply-all-btn"
            @click="emit('apply-all-changes', message.id)"
          >
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <polyline points="20 6 9 17 4 12" />
            </svg>
            全部应用
          </button>
        </template>

        <!-- 纯文本消息（用户消息、系统消息等） -->
        <template v-else>
          <div
            v-if="message.content"
            class="message-text"
            :class="{ 'is-streaming': message.isStreaming }"
            v-html="formattedContent"
          />
        </template>
      </div>

      <!-- 时间戳 -->
      <div class="message-time">
        {{ formatTime(message.timestamp) }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ChatMessage } from '@/types/ai-chat'
import { useMarkdown } from '@/composables/useMarkdown'
import AIIcon from '@/components/common/AIIcon.vue'
import SectionChangeCard from './suggestion-cards/SectionChangeCard.vue'

interface Props {
  message: ChatMessage
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'preview-image': [url: string]
  'apply-single-change': [messageId: string, index: number]
  'ignore-change': [messageId: string, index: number]
  'apply-all-changes': [messageId: string]
}>()

const { renderMarkdown } = useMarkdown()

// Markdown 渲染工具（分段和纯文本共用）
const formattedContent = computed(() => renderMarkdown(props.message.content))

// 分片文本的 Markdown 渲染
function renderSegmentMarkdown(text: string): string {
  return renderMarkdown(text)
}

function handleImageClick(url: string) {
  emit('preview-image', url)
}

// 是否有多个 pending 卡片（控制"全部应用"按钮）
const hasMultiplePending = computed(() => {
  if (!props.message.actions) return false
  const pendingCount = props.message.actions.filter(a => a.status === 'pending' || !a.status).length
  return pendingCount >= 2
})

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
  min-width: 0;

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

    .message-body {
      align-items: flex-end;
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

    .message-body {
      align-items: flex-start;
    }
  }

  &.message-system {
    align-self: center;
    max-width: 100%;

    .message-content {
      background: rgba($color-accent, 0.08);
      border: 1px solid rgba($color-accent, 0.15);
      border-radius: $radius-sm;
      padding: $spacing-xs $spacing-md;
      text-align: center;
      font-size: $text-xs;
      color: $color-accent;
      line-height: 1.5;
    }

    .message-body {
      align-items: center;
    }

    .message-time {
      display: none;
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

.message-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 100%;
  min-width: 0;
}

.message-content {
  padding: $spacing-md;
  border-radius: $radius-md;
  min-width: 0;

  :deep(p) {
    margin: 0 0 $spacing-sm 0;

    &:last-child {
      margin-bottom: 0;
    }
  }

  :deep(ul) {
    margin: $spacing-sm 0;
    padding-left: $spacing-lg;
    list-style: disc;
  }

  :deep(ol) {
    margin: $spacing-sm 0;
    padding-left: $spacing-lg;
    list-style: decimal;
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
      white-space: pre;
      display: block;
    }
  }

  :deep(table) {
    width: 100%;
    max-width: 100%;
    border-collapse: collapse;
    margin: $spacing-sm 0;
    font-size: $text-sm;
    display: block;
    overflow-x: auto;

    th,
    td {
      padding: $spacing-xs $spacing-sm;
      border: 1px solid rgba(255, 255, 255, 0.1);
      text-align: left;
    }

    th {
      background: rgba(0, 0, 0, 0.3);
      font-weight: $weight-medium;
      color: $color-text-primary;
    }

    tr:nth-child(2n) {
      background: rgba(255, 255, 255, 0.02);
    }

    tr:hover {
      background: rgba($color-accent, 0.05);
    }
  }

  // 标题样式 - 适配聊天场景的紧凑设计
  :deep(h1) {
    font-size: $text-xl;
    font-weight: $weight-bold;
    margin: $spacing-lg 0 $spacing-sm;
    color: $color-text-primary;
  }

  :deep(h2) {
    font-size: $text-lg;
    font-weight: $weight-semibold;
    margin: $spacing-md 0 $spacing-sm;
    color: $color-text-primary;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    padding-bottom: $spacing-xs;
  }

  :deep(h3) {
    font-size: $text-base;
    font-weight: $weight-semibold;
    margin: $spacing-md 0 $spacing-xs;
    color: $color-text-primary;
  }

  :deep(h4) {
    font-size: $text-sm;
    font-weight: $weight-medium;
    margin: $spacing-sm 0 $spacing-xs;
    color: $color-text-secondary;
  }

  :deep(h5),
  :deep(h6) {
    font-size: $text-xs;
    font-weight: $weight-medium;
    margin: $spacing-sm 0;
    color: $color-text-tertiary;
  }

  // 分割线样式
  :deep(hr) {
    border: none;
    border-top: 1px solid rgba(255, 255, 255, 0.1);
    margin: $spacing-md 0;
  }
}

.message-images {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-xs;
  margin-bottom: $spacing-sm;

  .message-image {
    width: calc(50% - $spacing-xs / 2);
    max-width: 200px;
    position: relative;
    cursor: pointer;
    border-radius: $radius-sm;
    overflow: hidden;

    img {
      width: 100%;
      aspect-ratio: 1;
      object-fit: cover;
      transition: transform $transition-fast;
    }

    .image-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.4);
      display: flex;
      align-items: center;
      justify-content: center;
      opacity: 0;
      transition: opacity $transition-fast;
      color: $color-text-primary;
    }

    &:hover {
      img {
        transform: scale(1.02);
      }

      .image-overlay {
        opacity: 1;
      }
    }
  }
}

.message-text {
  line-height: $leading-relaxed;
  word-break: break-word;
}

// 流式输出光标 - 使用伪元素实现，紧跟在最后一个段落后面
.message-text.is-streaming {
  :deep(p:last-child) {
    display: inline;

    &::after {
      content: '';
      display: inline-block;
      width: 2px;
      height: 1em;
      background: $color-accent;
      margin-left: 2px;
      animation: blink 1s infinite;
      vertical-align: text-bottom;
    }
  }
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
  font-size: 10px;
  color: $color-text-tertiary;
  padding: 0 $spacing-xs;
  opacity: 0.6;
}

// 用户消息时间在右边
.message-user .message-time {
  text-align: right;
}

// AI消息时间在左边
.message-assistant .message-time {
  text-align: left;
}

// 内联操作卡片
.inline-action-card {
  margin: $spacing-sm 0;
}

.apply-all-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-xs;
  padding: $spacing-sm $spacing-lg;
  margin-top: $spacing-sm;
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-bg-primary;
  background: $color-accent;
  border: none;
  border-radius: $radius-sm;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    background: $color-accent-light;
  }

  &:active {
    transform: scale(0.98);
  }
}
</style>
