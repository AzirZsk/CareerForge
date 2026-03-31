<template>
  <Teleport to="body">
    <div class="dialog-overlay" @click.self="$emit('close')">
      <div class="dialog-content">
        <header class="dialog-header">
          <h2>创建面试</h2>
          <button class="close-btn" @click="$emit('close')">×</button>
        </header>

        <form class="dialog-form" @submit.prevent="handleSubmit">
          <div class="form-group">
            <label class="form-label required">公司名称</label>
            <input
              v-model="form.companyName"
              type="text"
              class="form-input"
              placeholder="请输入公司名称"
              required
            />
          </div>

          <div class="form-group">
            <label class="form-label required">目标岗位</label>
            <input
              v-model="form.position"
              type="text"
              class="form-input"
              placeholder="请输入目标岗位"
              required
            />
          </div>

          <div class="form-group">
            <label class="form-label required">面试时间</label>
            <DateTimePicker v-model="form.interviewDate" />
          </div>

          <div class="form-group">
            <label class="form-label">JD 内容（可选）</label>
            <textarea
              v-model="form.jdContent"
              class="form-textarea"
              placeholder="粘贴职位描述..."
              rows="4"
            ></textarea>
          </div>

          <div class="form-group">
            <label class="form-label">备注</label>
            <textarea
              v-model="form.notes"
              class="form-textarea"
              placeholder="其他备注..."
              rows="2"
            ></textarea>
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
            {{ submitting ? '创建中...' : '创建' }}
          </button>
        </footer>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { createInterview } from '@/api/interview-center'
import type { CreateInterviewRequest } from '@/types/interview-center'
import DateTimePicker from '@/components/common/DateTimePicker.vue'

const emit = defineEmits<{
  close: []
  created: [id: string]
}>()

const submitting = ref(false)

const form = reactive<CreateInterviewRequest>({
  companyName: '',
  position: '',
  interviewDate: '',
  jdContent: '',
  notes: ''
})

const isFormValid = computed(() => {
  return form.companyName.trim() && form.position.trim() && form.interviewDate
})

async function handleSubmit() {
  if (submitting.value || !isFormValid.value) return

  submitting.value = true
  try {
    const result = await createInterview(form)
    emit('created', result.id)
  } catch (error) {
    console.error('创建面试失败:', error)
    alert('创建失败，请稍后重试')
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
  z-index: $z-overlay;
}

.dialog-content {
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  width: 90%;
  max-width: 500px;
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

.form-input, .form-textarea {
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
