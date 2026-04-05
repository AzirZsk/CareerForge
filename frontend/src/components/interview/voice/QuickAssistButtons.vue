<template>
  <div class="quick-assist-buttons">
    <div class="assist-header">
      <span class="title">💡 快捷求助</span>
      <span class="remaining">剩余 {{ remaining }}/{{ limit }} 次</span>
    </div>

    <div class="button-group">
      <button
        v-for="button in assistButtons"
        :key="button.type"
        class="assist-btn"
        :disabled="isDisabled"
        @click="handleAssist(button.type)"
      >
        <span class="btn-icon">{{ button.icon }}</span>
        <span class="btn-text">{{ button.label }}</span>
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
const assistButtons: Array<{ type: AssistType; icon: string; label: string }> = [
  { type: 'give_hints', icon: '🎯', label: '给我思路' },
  { type: 'explain_concept', icon: '📖', label: '解释概念' },
  { type: 'polish_answer', icon: '✍️', label: '帮我润色' },
  { type: 'free_question', icon: '💬', label: '自由提问' }
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

.button-group {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-sm;

  .assist-btn {
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
