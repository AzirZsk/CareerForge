<template>
  <div class="create-interview-page">
    <header class="page-header">
      <button class="back-btn" @click="goBack">← 返回</button>
      <h1>创建面试</h1>
    </header>

    <form class="create-form" @submit.prevent="handleSubmit">
      <div class="form-section">
        <h2>基本信息</h2>

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
          <label class="form-label required">轮次类型</label>
          <select v-model="form.roundType" class="form-select" required>
            <option v-for="(label, type) in ROUND_TYPE_LABELS" :key="type" :value="type">
              {{ label }}
            </option>
          </select>
        </div>

        <div v-if="form.roundType === 'custom'" class="form-group">
          <label class="form-label required">轮次名称</label>
          <input
            v-model="form.roundName"
            type="text"
            class="form-input"
            placeholder="请输入轮次名称"
            :required="form.roundType === 'custom'"
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
            placeholder="粘贴职位描述，可触发 AI 分析..."
            rows="6"
          ></textarea>
        </div>

        <div class="form-group">
          <label class="form-label">备注</label>
          <textarea
            v-model="form.notes"
            class="form-textarea"
            placeholder="其他备注信息..."
            rows="3"
          ></textarea>
        </div>
      </div>

      <div class="form-actions">
        <button type="button" class="btn btn-secondary" @click="goBack">取消</button>
        <button type="submit" class="btn btn-primary" :disabled="submitting">
          {{ submitting ? '创建中...' : '创建面试' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { createInterview } from '@/api/interview-center'
import type { CreateInterviewRequest, RoundType } from '@/types/interview-center'
import { ROUND_TYPE_LABELS } from '@/types/interview-center'
import DateTimePicker from '@/components/common/DateTimePicker.vue'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const toast = useToast()
const submitting = ref(false)

const form = reactive<CreateInterviewRequest>({
  companyName: '',
  position: '',
  interviewDate: '',
  roundType: 'technical_1' as RoundType,
  roundName: '',
  jdContent: '',
  notes: ''
})

function goBack() {
  router.push('/interview-center')
}

async function handleSubmit() {
  if (submitting.value) return

  // 自定义轮次必须填写名称
  if (form.roundType === 'custom' && !form.roundName?.trim()) {
    toast.warning('请输入轮次名称')
    return
  }

  submitting.value = true
  try {
    const result = await createInterview(form)
    router.push(`/interview-center/${result.id}`)
  } catch (error) {
    console.error('创建面试失败:', error)
    toast.error('创建失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.create-interview-page {
  padding: $spacing-xl;
  max-width: 700px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: $spacing-lg;
  margin-bottom: $spacing-xl;

  h1 {
    font-size: 1.5rem;
    font-weight: 700;
    color: $color-text-primary;
  }
}

.back-btn {
  background: none;
  border: none;
  color: $color-text-secondary;
  cursor: pointer;
  font-size: 1rem;

  &:hover {
    color: $color-text-primary;
  }
}

.create-form {
  display: flex;
  flex-direction: column;
  gap: $spacing-xl;
}

.form-section {
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  padding: $spacing-xl;

  h2 {
    font-size: 1.125rem;
    font-weight: 600;
    color: $color-text-primary;
    margin-bottom: $spacing-lg;
  }
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
.form-select,
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

.form-select {
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2371717a' d='M6 8L1 3h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  padding-right: 32px;
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: $spacing-md;
}

.btn {
  padding: $spacing-md $spacing-xl;
  font-size: 0.875rem;
  font-weight: 500;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all 0.2s;

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.btn-primary {
  background: $gradient-gold;
  color: $color-bg-deep;

  &:not(:disabled):hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 16px rgba($color-accent, 0.3);
  }
}

.btn-secondary {
  background: $color-bg-tertiary;
  color: $color-text-secondary;

  &:hover {
    color: $color-text-primary;
  }
}
</style>
