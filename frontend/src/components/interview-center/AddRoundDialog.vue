<template>
  <div class="dialog-overlay" @click.self="$emit('close')">
    <div class="dialog-content">
      <header class="dialog-header">
        <h2>添加面试轮次</h2>
        <button class="close-btn" @click="$emit('close')">×</button>
      </header>

      <form class="dialog-form" @submit.prevent="handleSubmit">
        <div class="form-group">
          <label class="form-label required">轮次类型</label>
          <select v-model="form.roundType" class="form-select" required>
            <option value="technical_1">技术一面</option>
            <option value="technical_2">技术二面</option>
            <option value="hr">HR面</option>
            <option value="director">总监面</option>
            <option value="cto">CTO/VP面</option>
            <option value="final">终面</option>
            <option value="custom">自定义</option>
          </select>
        </div>

        <div v-if="form.roundType === 'custom'" class="form-group">
          <label class="form-label required">轮次名称</label>
          <input
            v-model="form.roundName"
            type="text"
            class="form-input"
            placeholder="请输入轮次名称"
            required
          />
        </div>

        <div class="form-group">
          <label class="form-label">预定时间</label>
          <input
            v-model="form.scheduledDate"
            type="datetime-local"
            class="form-input"
          />
        </div>

        <div class="form-group">
          <label class="form-label">备注</label>
          <textarea
            v-model="form.notes"
            class="form-textarea"
            placeholder="轮次相关备注..."
            rows="3"
          ></textarea>
        </div>
      </form>

      <footer class="dialog-footer">
        <button type="button" class="btn btn-secondary" @click="$emit('close')">取消</button>
        <button
          type="button"
          class="btn btn-primary"
          :disabled="submitting"
          @click="handleSubmit"
        >
          {{ submitting ? '添加中...' : '添加' }}
        </button>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { addRound } from '@/api/interview-center'
import type { AddRoundRequest } from '@/types/interview-center'

const props = defineProps<{
  interviewId: string
}>()

const emit = defineEmits<{
  close: []
  added: []
}>()

const submitting = ref(false)

const form = reactive<AddRoundRequest>({
  roundType: 'technical_1',
  roundName: '',
  scheduledDate: '',
  notes: ''
})

// 重置表单
watch(() => props.interviewId, () => {
  form.roundType = 'technical_1'
  form.roundName = ''
  form.scheduledDate = ''
  form.notes = ''
}, { immediate: true })

async function handleSubmit() {
  if (submitting.value) return

  // 自定义类型必须填写名称
  if (form.roundType === 'custom' && !form.roundName?.trim()) {
    alert('请输入轮次名称')
    return
  }

  submitting.value = true
  try {
    await addRound(props.interviewId, form)
    emit('added')
    emit('close')
  } catch (error) {
    console.error('添加轮次失败:', error)
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
  z-index: 1000;
}

.dialog-content {
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  width: 90%;
  max-width: 450px;
  max-height: 90vh;
  overflow-y: auto;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-lg;
  border-bottom: 1px solid $color-bg-tertiary;

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

.form-input, .form-select, .form-textarea {
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
  min-height: 80px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: $spacing-md;
  padding: $spacing-lg;
  border-top: 1px solid $color-bg-tertiary;
}
</style>
