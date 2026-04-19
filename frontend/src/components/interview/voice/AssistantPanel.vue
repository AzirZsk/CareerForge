<template>
  <div class="assistant-panel">
    <!-- 面板头部 -->
    <div class="panel-header">
      <div class="header-left">
        <font-awesome-icon icon="fa-solid fa-robot" class="icon" />
        <span class="title">AI 助手</span>
      </div>
      <button class="close-btn" @click="handleReturn">返回面试</button>
    </div>

    <!-- 助手回复内容 -->
    <div class="panel-content">
      <!-- 加载状态 -->
      <div v-if="isLoading" class="loading-state">
        <div class="spinner"></div>
        <p>正在思考中...</p>
      </div>

      <!-- 回复内容 -->
      <div v-else-if="content" class="response-content">
        <div class="text-content" v-html="formattedContent"></div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <font-awesome-icon icon="fa-solid fa-comment" class="empty-icon" />
        <p>助手回复将在这里显示...</p>
      </div>
    </div>

    <!-- 继续求助按钮 -->
    <div class="panel-footer">
      <QuickAssistButtons
        :remaining="assistRemaining"
        :limit="assistLimit"
        @assist="handleAssist"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import QuickAssistButtons from './QuickAssistButtons.vue'
import type { AssistType } from '@/types/interview-voice'

// Props
interface Props {
  content?: string
  isLoading?: boolean
  assistRemaining?: number
  assistLimit?: number
}

const props = withDefaults(defineProps<Props>(), {
  content: '',
  isLoading: false,
  assistRemaining: 5,
  assistLimit: 5
})

// Emits
const emit = defineEmits<{
  return: []
  assist: [type: AssistType, question?: string]
}>()

// 格式化内容（支持 Markdown）
const formattedContent = computed(() => {
  if (!props.content) return ''
  return props.content
    .split('\n')
    .map(line => `<p>${line}</p>`)
    .join('')
})

// 返回面试
function handleReturn() {
  emit('return')
}

// 处理求助
function handleAssist(type: AssistType, question?: string) {
  emit('assist', type, question)
}
</script>

<style scoped lang="scss">
.assistant-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: $gradient-card;
  border-radius: $radius-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-md $spacing-lg;
  background: $color-bg-tertiary;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);

  .header-left {
    display: flex;
    align-items: center;
    gap: $spacing-sm;

    .icon {
      font-size: 1.25rem;
    }

    .title {
      font-weight: $weight-semibold;
      font-size: $text-base;
      color: $color-text-primary;
    }
  }

  .close-btn {
    padding: $spacing-sm $spacing-md;
    background: $color-accent;
    color: $color-bg-deep;
    border: none;
    border-radius: $radius-md;
    cursor: pointer;
    font-size: $text-sm;
    font-weight: $weight-medium;
    transition: all $transition-fast;

    &:hover {
      opacity: 0.9;
      transform: translateY(-1px);
    }
  }
}

.panel-content {
  flex: 1;
  overflow-y: auto;
  padding: $spacing-lg;

  .loading-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    gap: $spacing-md;

    .spinner {
      width: 40px;
      height: 40px;
      border: 3px solid $color-border;
      border-top-color: $color-accent;
      border-radius: 50%;
      animation: spin 1s linear infinite;
    }

    p {
      color: $color-text-secondary;
      font-size: $text-sm;
    }
  }

  .response-content {
    .text-content {
      line-height: $leading-relaxed;
      color: $color-text-primary;
      font-size: $text-sm;

      p {
        margin-bottom: $spacing-md;
      }
    }
  }

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    gap: $spacing-md;
    color: $color-text-secondary;

    .empty-icon {
      font-size: 3rem;
      opacity: 0.5;
    }

    p {
      font-size: $text-sm;
    }
  }
}

.panel-footer {
  padding: $spacing-md $spacing-lg;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
