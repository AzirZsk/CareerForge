<template>
  <div class="quick-assist-buttons">
    <div class="assist-header">
      <span class="title"><font-awesome-icon icon="fa-solid fa-lightbulb" /> 快捷求助</span>
      <span class="remaining">剩余 {{ remaining }}/{{ limit }} 次</span>
    </div>
    <p class="assist-desc">点击后面试将暂停，AI 助手会实时帮助你</p>

    <div class="button-group">
      <button
        v-for="button in assistButtons"
        :key="button.type"
        class="assist-btn"
        :disabled="isDisabled"
        @click="handleAssist(button.type)"
      >
        <font-awesome-icon class="btn-icon" :icon="button.icon" />
        <span class="btn-text">{{ button.label }}</span>
        <font-awesome-icon class="btn-question" icon="fa-solid fa-circle-question" />
        <span class="btn-tooltip">{{ button.desc }}</span>
      </button>
    </div>

    <!-- 自由提问输入框 -->
    <div v-if="showFreeInput" class="free-input">
      <textarea
        v-model="freeQuestion"
        placeholder="输入你的问题..."
        rows="2"
        @keydown.enter.ctrl="submitFreeQuestion"
      />
      <div class="input-actions">
        <button class="cancel-btn" @click="showFreeInput = false">取消</button>
        <button class="submit-btn" @click="submitFreeQuestion" :disabled="!freeQuestion.trim()">
          发送
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { AssistType } from '@/types/interview-voice'

// Props
interface Props {
  remaining?: number
  limit?: number
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  remaining: 5,
  limit: 5,
  disabled: false
})

// Emits
const emit = defineEmits<{
  (e: 'assist', type: AssistType, question?: string): void
}>()

// 求助按钮配置 - 使用正确的 AssistType 值
const assistButtons: Array<{ type: AssistType; icon: string; label: string; desc: string }> = [
  { type: 'give_hints', icon: 'fa-solid fa-bullseye', label: '给我思路', desc: 'AI 帮你梳理答题方向' },
  { type: 'explain_concept', icon: 'fa-solid fa-book-open', label: '解释概念', desc: 'AI 为你解释专业术语' },
  { type: 'free_question', icon: 'fa-solid fa-comment', label: '自由提问', desc: '输入问题，AI 为你解答' }
]

// 自由提问
const showFreeInput = ref(false)
const freeQuestion = ref('')

// 是否禁用
const isDisabled = computed(() => props.disabled || props.remaining <= 0)

// 处理求助
function handleAssist(type: AssistType) {
  if (type === 'free_question') {
    showFreeInput.value = true
  } else {
    emit('assist', type)
  }
}

// 提交自由提问
function submitFreeQuestion() {
  if (freeQuestion.value.trim()) {
    emit('assist', 'free_question', freeQuestion.value.trim())
    freeQuestion.value = ''
    showFreeInput.value = false
  }
}
</script>

<style scoped lang="scss">
.quick-assist-buttons {
  padding: $spacing-md;
  background: $gradient-card;
  border-radius: $radius-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.assist-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-md;

  .title {
    font-weight: $weight-semibold;
    font-size: $text-sm;
    color: $color-text-primary;
  }

  .remaining {
    font-size: $text-xs;
    color: $color-text-secondary;
    background: $color-bg-tertiary;
    padding: $spacing-xs $spacing-sm;
    border-radius: $radius-sm;
  }
}

.assist-desc {
  font-size: $text-xs;
  color: $color-text-tertiary;
  margin-top: $spacing-xs;
  margin-bottom: $spacing-sm;
}

.button-group {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-sm;

  .assist-btn {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: $spacing-xs;
    padding: $spacing-md $spacing-sm;
    background: $color-bg-primary;
    border: 1px solid $color-border;
    border-radius: $radius-md;
    cursor: pointer;
    transition: all $transition-fast;

    &:hover:not(:disabled) {
      border-color: $color-accent;
      background: rgba($color-accent, 0.1);
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .btn-icon {
      font-size: 1.25rem;
    }

    .btn-text {
      font-size: $text-xs;
      color: $color-text-secondary;
    }

    .btn-question {
      position: absolute;
      top: 6px;
      right: 6px;
      font-size: 12px;
      color: $color-text-tertiary;
      opacity: 0.6;
      transition: opacity $transition-fast;
    }

    .btn-tooltip {
      position: absolute;
      bottom: calc(100% + 8px);
      left: 50%;
      transform: translateX(-50%) translateY(4px);
      padding: $spacing-xs $spacing-sm;
      background: $color-bg-elevated;
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: $radius-sm;
      font-size: $text-xs;
      color: $color-text-secondary;
      white-space: nowrap;
      opacity: 0;
      visibility: hidden;
      transition: all $transition-fast;
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
      z-index: $z-tooltip;
      pointer-events: none;

      &::after {
        content: '';
        position: absolute;
        top: 100%;
        left: 50%;
        transform: translateX(-50%);
        border: 5px solid transparent;
        border-top-color: $color-bg-elevated;
      }
    }

    &:hover {
      .btn-question {
        opacity: 1;
      }

      .btn-tooltip {
        opacity: 1;
        visibility: visible;
        transform: translateX(-50%) translateY(0);
      }
    }
  }
}

.free-input {
  margin-top: $spacing-md;
  padding-top: $spacing-md;
  border-top: 1px solid $color-border;

  textarea {
    width: 100%;
    padding: $spacing-sm $spacing-md;
    background: $color-bg-primary;
    border: 1px solid $color-border;
    border-radius: $radius-md;
    resize: none;
    font-size: $text-sm;
    color: $color-text-primary;

    &::placeholder {
      color: $color-text-tertiary;
    }

    &:focus {
      outline: none;
      border-color: $color-accent;
    }
  }

  .input-actions {
    display: flex;
    justify-content: flex-end;
    gap: $spacing-sm;
    margin-top: $spacing-sm;

    button {
      padding: $spacing-xs $spacing-md;
      border-radius: $radius-sm;
      font-size: $text-xs;
      cursor: pointer;
      transition: all $transition-fast;
    }

    .cancel-btn {
      background: transparent;
      border: 1px solid $color-border;
      color: $color-text-secondary;

      &:hover {
        background: rgba(255, 255, 255, 0.05);
      }
    }

    .submit-btn {
      background: $color-accent;
      border: none;
      color: $color-bg-deep;
      font-weight: $weight-medium;

      &:hover:not(:disabled) {
        opacity: 0.9;
      }

      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
    }
  }
}
</style>
