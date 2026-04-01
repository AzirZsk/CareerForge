<template>
  <div class="dialog-overlay" @click.self="$emit('close')">
    <div class="dialog-content">
      <header class="dialog-header">
        <h2>编辑面试信息</h2>
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
          <label class="form-label">JD 内容</label>
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

        <div class="form-group">
          <label class="form-label">面试状态</label>
          <select v-model="form.status" class="form-select">
            <option v-for="(label, key) in statusOptions" :key="key" :value="key">
              {{ label }}
            </option>
          </select>
        </div>

        <div class="form-group">
          <label class="form-label">最终结果</label>
          <select v-model="form.overallResult" class="form-select">
            <option value="">未设置</option>
            <option v-for="(label, key) in resultOptions" :key="key" :value="key">
              {{ label }}
            </option>
          </select>
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
          {{ submitting ? '保存中...' : '保存' }}
        </button>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue'
import { updateInterview } from '@/api/interview-center'
import type { UpdateInterviewRequest, InterviewDetail } from '@/types/interview-center'
import { INTERVIEW_STATUS_LABELS, INTERVIEW_RESULT_LABELS } from '@/types/interview-center'
import DateTimePicker from '@/components/common/DateTimePicker.vue'

const props = defineProps<{
  interview: InterviewDetail
}>()

const emit = defineEmits<{
  close: []
  saved: []
}>()

const submitting = ref(false)

const form = reactive<UpdateInterviewRequest>({
  companyName: '',
  position: '',
  interviewDate: '',
  jdContent: '',
  notes: '',
  status: undefined,
  overallResult: undefined
})

const isFormValid = computed(() => {
  return !!form.companyName?.trim() && !!form.position?.trim() && !!form.interviewDate
})

const statusOptions = INTERVIEW_STATUS_LABELS
const resultOptions = INTERVIEW_RESULT_LABELS

// 监听面试数据变化，填充表单
watch(() => props.interview, (interview) => {
  if (interview) {
    form.companyName = interview.companyName || ''
    form.position = interview.position || ''
    form.interviewDate = interview.interviewDate || ''
    form.jdContent = interview.jdContent || ''
    form.notes = interview.notes || ''
    form.status = interview.status
    form.overallResult = interview.overallResult
  }
}, { immediate: true })

async function handleSubmit() {
  if (submitting.value || !isFormValid.value) return

  submitting.value = true
  try {
    await updateInterview(props.interview.id, form)
    emit('saved')
    emit('close')
  } catch (error) {
    console.error('更新面试信息失败:', error)
    alert('保存失败，请稍后重试')
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

.form-input, .form-textarea, .form-select {
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

.form-select {
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23a1a1aa' d='M6 8L1 3h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  padding-right: 36px;
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
