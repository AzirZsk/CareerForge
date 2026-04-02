<template>
  <Teleport to="body">
    <div class="dialog-overlay" @click.self="$emit('close')">
      <div class="dialog-content">
        <header class="dialog-header">
          <h2>添加准备事项</h2>
          <button class="close-btn" @click="$emit('close')">×</button>
        </header>

        <form class="dialog-form" @submit.prevent="handleSubmit">
          <div class="form-group">
            <label class="form-label required">事项标题</label>
            <input
              v-model="form.title"
              type="text"
              class="form-input"
              placeholder="例如：复习Spring Boot原理"
              required
            />
          </div>

          <div class="form-group">
            <label class="form-label">详细内容</label>
            <textarea
              v-model="form.content"
              class="form-textarea"
              placeholder="具体要做什么..."
              rows="4"
            ></textarea>
          </div>

          <div class="form-group">
            <label class="form-label">优先级</label>
            <div class="priority-selector">
              <button
                v-for="(config, key) in PRIORITY_CONFIG"
                :key="key"
                type="button"
                class="priority-option"
                :class="{ active: form.priority === key }"
                @click="form.priority = key as PreparationPriority"
              >
                <span class="priority-icon">{{ config.icon }}</span>
                <span class="priority-label">{{ config.label }}</span>
              </button>
            </div>
          </div>
        </form>

        <footer class="dialog-footer">
          <button type="button" class="btn btn-secondary" @click="$emit('close')">取消</button>
          <button
            type="button"
            class="btn btn-primary"
            :disabled="submitting || !isFormValid"
            @click="handleSubmit"
          >
            {{ submitting ? '添加中...' : '添加' }}
          </button>
        </footer>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useScrollLock } from '@vueuse/core'
import { addPreparation } from '@/api/interview-center'
import type {
  AddPreparationRequest,
  PreparationPriority
} from '@/types/interview-center'
import { PRIORITY_CONFIG } from '@/types/interview-center'

const props = defineProps<{
  interviewId: string
}>()

const emit = defineEmits<{
  close: []
  added: []
}>()

const submitting = ref(false)

// 锁定背景滚动，防止滚动穿透
const isScrollLocked = useScrollLock(document.body)

const form = reactive<AddPreparationRequest>({
  title: '',
  content: '',
  priority: 'recommended' as PreparationPriority
})

const isFormValid = computed(() => {
  return form.title.trim().length > 0
})

// 组件挂载时锁定滚动
onMounted(() => {
  isScrollLocked.value = true
})

// 组件卸载时解锁滚动
onUnmounted(() => {
  isScrollLocked.value = false
})

async function handleSubmit() {
  if (submitting.value || !isFormValid.value) return

  submitting.value = true
  try {
    await addPreparation(props.interviewId, form)
    // 重置表单
    form.title = ''
    form.content = ''
    form.priority = 'recommended'
    emit('added')
    emit('close')
  } catch (error) {
    console.error('添加准备事项失败:', error)
    alert('添加失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: $z-modal-overlay;
}

.dialog-content {
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  width: 90%;
  max-width: 450px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-lg;
  border-bottom: 1px solid $color-bg-tertiary;
  flex-shrink: 0;

  h2 {
    font-size: 1.25rem;
    font-weight: 600;
    color: $color-text-primary;
  }

  .close-btn {
    background: none;
    border: none;
    color: $color-text-tertiary;
    font-size: 1.5rem;
    cursor: pointer;
    line-height: 1;

    &:hover {
      color: $color-text-primary;
    }
  }
}

.dialog-form {
  padding: $spacing-lg;
  flex: 1;
  overflow-y: auto;
}

.form-group {
  margin-bottom: $spacing-lg;

  &:last-child {
    margin-bottom: 0;
  }
}

.form-label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  color: $color-text-secondary;
  margin-bottom: $spacing-sm;

  &.required::after {
    content: ' *';
    color: $color-error;
  }
}

.form-input,
.form-textarea {
  width: 100%;
  padding: $spacing-md;
  background: $color-bg-tertiary;
  border: 1px solid $color-bg-tertiary;
  border-radius: $radius-md;
  color: $color-text-primary;
  font-size: 0.875rem;
  transition: border-color 0.2s;

  &:focus {
    outline: none;
    border-color: $color-accent;
  }

  &::placeholder {
    color: $color-text-tertiary;
  }
}

.form-textarea {
  resize: vertical;
  min-height: 100px;
}

.priority-selector {
  display: flex;
  gap: $spacing-sm;
}

.priority-option {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-md;
  background: $color-bg-tertiary;
  border: 2px solid transparent;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: $color-bg-elevated;
  }

  &.active {
    border-color: $color-accent;
    background: rgba($color-accent, 0.1);
  }
}

.priority-icon {
  font-size: 1.25rem;
}

.priority-label {
  font-size: 0.8125rem;
  color: $color-text-secondary;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: $spacing-md;
  padding: $spacing-lg;
  border-top: 1px solid $color-bg-tertiary;
  flex-shrink: 0;
}
</style>
