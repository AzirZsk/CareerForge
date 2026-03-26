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
        <AIIcon :size="20" />
      </template>
    </div>

    <!-- 消息主体 -->
    <div class="message-body">
      <!-- 消息内容 -->
      <div class="message-content">
        <!-- 多图预览 -->
        <div v-if="message.imageUrls && message.imageUrls.length > 0" class="message-images">
          <div
            v-for="(url, index) in message.imageUrls"
            :key="index"
            class="message-image"
            @click="handleImageClick(url)"
          >
            <img :src="url" :alt="`图片 ${index + 1}`" />
            <div class="image-overlay">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                <line x1="11" y1="8" x2="11" y2="14"></line>
                <line x1="8" y1="11" x2="14" y2="11"></line>
              </svg>
            </div>
          </div>
        </div>

        <!-- 文本内容 -->
        <div
          class="message-text"
          :class="{ 'is-streaming': message.isStreaming }"
          v-html="formattedContent"
        ></div>

        <!-- 建议卡片 -->
        <div v-if="message.suggestions && message.suggestions.length > 0" class="suggestions-container">
          <div v-for="(change, index) in message.suggestions" :key="index" class="suggestion-card">
            <div class="suggestion-header">
              <span class="change-type" :class="change.changeType">
                {{ getTypeLabel(change.changeType) }}
              </span>
              <span class="section-name">{{ change.sectionTitle || change.sectionType || '内容修改' }}</span>
            </div>
            <div class="suggestion-desc">{{ change.description }}</div>
            <div v-if="change.beforeContent || change.afterContent" class="suggestion-diff">
              <div v-if="change.beforeContent" class="diff-item diff-before">
                <span class="diff-label">修改前：</span>
                <div class="diff-content">{{ truncateContent(change.beforeContent) }}</div>
              </div>
              <div v-if="change.afterContent" class="diff-item diff-after">
                <span class="diff-label">修改后：</span>
                <div class="diff-content">{{ truncateContent(change.afterContent) }}</div>
              </div>
            </div>
          </div>
          <!-- 操作按钮 -->
          <button
            v-if="!message.applied"
            class="apply-btn"
            @click="handleApplySuggestion"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
            应用修改
          </button>
          <div v-else class="applied-status">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
            已应用
          </div>
        </div>
      </div>

      <!-- 时间戳 -->
      <div class="message-time">{{ formatTime(message.timestamp) }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ChatMessage } from '@/types/ai-chat'
import { useMarkdown } from '@/composables/useMarkdown'
import AIIcon from '@/components/common/AIIcon.vue'

interface Props {
  message: ChatMessage
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'preview-image': [url: string]
  'apply-suggestion': [messageId: string]
}>()

const { renderMarkdown } = useMarkdown()

const formattedContent = computed(() => renderMarkdown(props.message.content))

function handleImageClick(url: string) {
  emit('preview-image', url)
}

function handleApplySuggestion() {
  emit('apply-suggestion', props.message.id)
}

function getTypeLabel(type: string): string {
  const labels: Record<string, string> = {
    'update': '修改',
    'add': '新增',
    'delete': '删除'
  }
  return labels[type] || type
}

function truncateContent(content: string | undefined): string {
  if (!content) return ''
  try {
    const parsed = JSON.parse(content)
    const str = JSON.stringify(parsed, null, 2)
    return str.length > 150 ? str.slice(0, 150) + '...' : str
  } catch {
    return content.length > 150 ? content.slice(0, 150) + '...' : content
  }
}

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
      background: rgba($color-info, 0.1);
      border: 1px solid rgba($color-info, 0.2);
      text-align: center;
      font-size: $text-sm;
      color: $color-text-secondary;
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
}

.message-content {
  padding: $spacing-md;
  border-radius: $radius-md;

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
    }
  }
}

.message-images {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-xs;
  margin-bottom: $spacing-sm;

  .message-image {
    width: calc(50% - #{$spacing-xs / 2});
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

// 建议卡片容器
.suggestions-container {
  margin-top: $spacing-md;
  padding-top: $spacing-md;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.suggestion-card {
  padding: $spacing-sm $spacing-md;
  background: rgba(0, 0, 0, 0.2);
  border-radius: $radius-sm;
  border: 1px solid rgba(255, 255, 255, 0.04);
}

.suggestion-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-xs;
}

.change-type {
  padding: 2px 8px;
  border-radius: $radius-sm;
  font-size: $text-xs;
  font-weight: $weight-medium;

  &.update {
    background: rgba($color-accent, 0.2);
    color: $color-accent;
  }

  &.add {
    background: rgba($color-success, 0.2);
    color: $color-success;
  }

  &.delete {
    background: rgba($color-error, 0.2);
    color: $color-error;
  }
}

.section-name {
  font-size: $text-sm;
  color: $color-text-secondary;
  font-weight: $weight-medium;
}

.suggestion-desc {
  font-size: $text-sm;
  color: $color-text-tertiary;
  line-height: $leading-relaxed;
}

.suggestion-diff {
  margin-top: $spacing-sm;
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
}

.diff-item {
  .diff-label {
    font-size: $text-xs;
    color: $color-text-tertiary;
    display: block;
    margin-bottom: 2px;
  }

  .diff-content {
    font-size: $text-xs;
    font-family: monospace;
    white-space: pre-wrap;
    word-break: break-word;
    color: $color-text-secondary;
    background: rgba(0, 0, 0, 0.15);
    padding: $spacing-xs;
    border-radius: $radius-xs;
    max-height: 80px;
    overflow-y: auto;
  }
}

.diff-after {
  .diff-label {
    color: $color-accent;
  }

  .diff-content {
    color: $color-accent;
    border: 1px solid rgba($color-accent, 0.2);
  }
}

.apply-btn {
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

.applied-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-xs;
  padding: $spacing-sm $spacing-lg;
  margin-top: $spacing-sm;
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-success;
  background: rgba($color-success, 0.1);
  border: 1px solid rgba($color-success, 0.2);
  border-radius: $radius-sm;
}
</style>
