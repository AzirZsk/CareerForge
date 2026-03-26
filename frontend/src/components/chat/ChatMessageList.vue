<!--=====================================================
  聊天消息列表组件
  显示对话消息列表，支持自动滚动
  @author Azir
=====================================================-->

<template>
  <div ref="messageListRef" class="chat-message-list">
    <div class="messages-container">
      <ChatMessageItem
        v-for="message in messages"
        :key="message.id"
        :message="message"
        @preview-image="emit('preview-image', $event)"
        @apply-suggestion="emit('apply-suggestion', $event)"
      />

      <!-- 加载中提示 -->
      <div v-if="isStreaming && !hasStreamingMessage" class="loading-indicator">
        <span class="loading-text">正在思考</span>
        <div class="typing-dots">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, computed } from 'vue'
import type { ChatMessage } from '@/types/ai-chat'
import ChatMessageItem from './ChatMessageItem.vue'

interface Props {
  messages: ChatMessage[]
  isStreaming: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'preview-image': [url: string]
  'apply-suggestion': [messageId: string]
}>()

const messageListRef = ref<HTMLElement | null>(null)

// 检查是否有正在流式输出的消息
const hasStreamingMessage = computed(() => {
  return props.messages.some(m => m.isStreaming)
})

// 监听消息变化，自动滚动到底部
watch(
  () => props.messages.length,
  () => {
    nextTick(() => {
      scrollToBottom()
    })
  }
)

// 监听流式消息内容变化
watch(
  () => props.messages.map(m => m.content).join(''),
  () => {
    nextTick(() => {
      scrollToBottom()
    })
  }
)

function scrollToBottom() {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}
</script>

<style lang="scss" scoped>
.chat-message-list {
  flex: 1;
  overflow-y: auto;
  padding: $spacing-lg;
  overscroll-behavior: contain;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.1);
    border-radius: 3px;

    &:hover {
      background: rgba(255, 255, 255, 0.2);
    }
  }
}

.messages-container {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
  max-width: 900px;
  margin: 0 auto;
  width: 100%;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-md;
  background: $color-bg-tertiary;
  border-radius: $radius-md;
  width: fit-content;
}

.typing-dots {
  display: flex;
  gap: 4px;

  span {
    width: 6px;
    height: 6px;
    background: $color-accent;
    border-radius: 50%;
    animation: typing 1.4s infinite ease-in-out;

    &:nth-child(1) {
      animation-delay: -0.32s;
    }

    &:nth-child(2) {
      animation-delay: -0.16s;
    }
  }
}

.loading-text {
  font-size: $text-sm;
  color: $color-text-tertiary;
}

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0.8);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}
</style>
