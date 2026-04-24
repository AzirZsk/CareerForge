<template>
  <div class="assistant-panel">
    <!-- 面板头部 -->
    <div class="panel-header">
      <div class="header-left">
        <font-awesome-icon :icon="headerConfig.icon" class="icon" :style="{ color: headerConfig.color }" />
        <div class="header-text">
          <span class="title">{{ headerConfig.title }}</span>
          <span class="desc">{{ headerConfig.desc }}</span>
        </div>
      </div>
      <button class="close-btn" @click="handleReturn">返回面试</button>
    </div>

    <!-- 助手回复内容 -->
    <div class="panel-content">
      <!-- 加载状态 -->
      <div v-if="isLoading" class="loading-state">
        <div class="spinner"></div>
        <p>{{ loadingText }}</p>
      </div>

      <!-- 结构化内容 -->
      <div v-else-if="structuredData" class="response-content">
        <AssistHintsContent v-if="assistType === 'give_hints'" :data="structuredData.content" />
        <AssistExplainContent v-else-if="assistType === 'explain_concept'" :data="structuredData.content" />
        <AssistFreeQuestionContent v-else-if="assistType === 'free_question'" :data="structuredData.content" />
      </div>

      <!-- 错误状态 -->
      <div v-else-if="errorMessage" class="error-state">
        <font-awesome-icon icon="fa-solid fa-circle-exclamation" class="error-icon" />
        <p>{{ errorMessage }}</p>
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
import AssistHintsContent from './assist/AssistHintsContent.vue'
import AssistExplainContent from './assist/AssistExplainContent.vue'
import AssistFreeQuestionContent from './assist/AssistFreeQuestionContent.vue'
import type { AssistType, StructuredEventData } from '@/types/interview-voice'

// 类型头部配置
interface HeaderConfig {
  icon: string
  title: string
  desc: string
  color: string
}

const HEADER_MAP: Record<string, HeaderConfig> = {
  give_hints: { icon: 'fa-solid fa-lightbulb', title: '思路提示', desc: '帮你梳理答题方向', color: '#60a5fa' },
  explain_concept: { icon: 'fa-solid fa-book-open', title: '概念解析', desc: '用通俗语言解释术语', color: '#34d399' },
  free_question: { icon: 'fa-solid fa-comments', title: 'AI 回答', desc: '为你解答疑问', color: '#a1a1aa' }
}

const DEFAULT_HEADER: HeaderConfig = { icon: 'fa-solid fa-robot', title: 'AI 助手', desc: '', color: '#a1a1aa' }

const LOADING_TEXT_MAP: Record<string, string> = {
  give_hints: '正在分析问题，整理思路...',
  explain_concept: '正在解析概念...',
  free_question: '正在思考回答...'
}

// Props
interface Props {
  structuredData?: StructuredEventData | null
  assistType?: AssistType | null
  isLoading?: boolean
  assistRemaining?: number
  assistLimit?: number
  errorMessage?: string | null
}

const props = withDefaults(defineProps<Props>(), {
  structuredData: null,
  assistType: null,
  isLoading: false,
  assistRemaining: 5,
  assistLimit: 5,
  errorMessage: null
})

// Emits
const emit = defineEmits<{
  return: []
  assist: [type: AssistType, question?: string]
}>()

// 头部配置
const headerConfig = computed(() => {
  if (props.assistType && HEADER_MAP[props.assistType]) {
    return HEADER_MAP[props.assistType]
  }
  return DEFAULT_HEADER
})

// loading 文案
const loadingText = computed(() => {
  if (props.assistType && LOADING_TEXT_MAP[props.assistType]) {
    return LOADING_TEXT_MAP[props.assistType]
  }
  return '正在思考中...'
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

    .header-text {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    .title {
      font-weight: $weight-semibold;
      font-size: $text-base;
      color: $color-text-primary;
    }

    .desc {
      font-size: 12px;
      color: $color-text-tertiary;
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
    flex-shrink: 0;

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
    // 结构化组件自带样式
  }

  .error-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    gap: $spacing-sm;
    color: $color-error;

    .error-icon {
      font-size: 2rem;
      opacity: 0.6;
    }

    p {
      font-size: $text-sm;
      color: $color-text-secondary;
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
