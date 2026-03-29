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
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: 12px;
}

.assist-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;

  .title {
    font-weight: 600;
    color: var(--text-primary);
  }

  .remaining {
    font-size: 12px;
    color: var(--text-secondary);
    background: var(--bg-tertiary);
    padding: 4px 8px;
    border-radius: 12px;
  }
}

.button-group {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;

  .assist-btn {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    padding: 12px 8px;
    background: var(--bg-primary);
    border: 1px solid var(--border-color);
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover:not(:disabled) {
      border-color: var(--primary-color);
      background: var(--bg-hover);
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .btn-icon {
      font-size: 20px;
    }

    .btn-text {
      font-size: 12px;
      color: var(--text-secondary);
    }
  }
}

.free-input {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);

  textarea {
    width: 100%;
    padding: 8px 12px;
    background: var(--bg-primary);
    border: 1px solid var(--border-color);
    border-radius: 8px;
    resize: none;
    font-size: 14px;

    &:focus {
      outline: none;
      border-color: var(--primary-color);
    }
  }

  .input-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    margin-top: 8px;

    button {
      padding: 6px 12px;
      border-radius: 6px;
      font-size: 12px;
      cursor: pointer;
    }

    .cancel-btn {
      background: transparent;
      border: 1px solid var(--border-color);
      color: var(--text-secondary);
    }

    .submit-btn {
      background: var(--primary-color);
      border: none;
      color: white;

      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
    }
  }
}
</style>
