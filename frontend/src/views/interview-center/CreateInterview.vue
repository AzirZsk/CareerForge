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

      <div class="form-section">
        <h2>预设面试轮次（可选）</h2>

        <div class="rounds-config">
          <div v-for="(round, index) in form.rounds" :key="index" class="round-item">
            <select v-model="round.roundType" class="form-select">
              <option value="technical_1">技术一面</option>
              <option value="technical_2">技术二面</option>
              <option value="hr">HR 面</option>
              <option value="director">总监面</option>
              <option value="cto">CTO/VP 面</option>
              <option value="final">终面</option>
              <option value="custom">自定义</option>
            </select>
            <input
              v-if="round.roundType === 'custom'"
              v-model="round.roundName"
              type="text"
              class="form-input"
              placeholder="轮次名称"
            />
            <DateTimePicker v-model="round.scheduledDate" />
            <button type="button" class="btn-icon delete" @click="removeRound(index)">×</button>
          </div>

          <button type="button" class="btn btn-secondary add-round-btn" @click="addRound">
            + 添加轮次
          </button>
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
import type { CreateInterviewRequest } from '@/types/interview-center'
import DateTimePicker from '@/components/common/DateTimePicker.vue'

const router = useRouter()
const submitting = ref(false)

const form = reactive<CreateInterviewRequest>({
  companyName: '',
  position: '',
  interviewDate: '',
  jdContent: '',
  notes: '',
  rounds: []
})

function addRound() {
  form.rounds?.push({
    roundType: 'technical_1',
    scheduledDate: ''
  })
}

function removeRound(index: number) {
  form.rounds?.splice(index, 1)
}

function goBack() {
  router.push('/interview-center')
}

async function handleSubmit() {
  if (submitting.value) return

  submitting.value = true
  try {
    const result = await createInterview(form)
    router.push(`/interview-center/${result.id}`)
  } catch (error) {
    console.error('创建面试失败:', error)
    alert('创建失败，请稍后重试')
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
  min-height: 100px;
}

.rounds-config {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.round-item {
  display: flex;
  gap: $spacing-md;
  align-items: center;

  .form-select, .form-input {
    flex: 1;
  }

  .btn-icon.delete {
    background: none;
    border: none;
    color: $color-text-tertiary;
    font-size: 1.25rem;
    cursor: pointer;
    padding: $spacing-sm;

    &:hover {
      color: $color-error;
    }
  }
}

.add-round-btn {
  align-self: flex-start;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: $spacing-md;
}
</style>
