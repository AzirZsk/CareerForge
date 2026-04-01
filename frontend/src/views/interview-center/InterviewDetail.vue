<template>
  <div class="interview-detail-page" v-if="!loading && interview">
    <header class="page-header">
      <button class="back-btn" @click="goBack">← 返回</button>
      <div class="header-actions">
        <button class="btn btn-secondary" @click="showEditDialog = true">编辑</button>
        <button class="btn btn-danger" @click="handleDelete">删除</button>
      </div>
    </header>

    <div class="detail-content">
      <!-- 弹窗组件 -->
      <AddPreparationDialog
        v-if="showAddPreparationDialog"
        :interview-id="interview.id"
        @close="showAddPreparationDialog = false"
        @added="handlePreparationAdded"
      />
      <ReviewNoteDialog
        v-if="showReviewDialog"
        :interview-id="interview.id"
        :existing-note="interview.reviewNote"
        @close="showReviewDialog = false"
        @saved="handleReviewSaved"
      />
      <EditInterviewDialog
        v-if="showEditDialog"
        :interview="interview"
        @close="showEditDialog = false"
        @saved="handleInterviewUpdated"
      />
      <section class="basic-info">
        <div class="info-header">
          <span class="source-badge" :class="interview.source">
            {{ interview.source === 'real' ? '真实面试' : '模拟面试' }}
          </span>
          <div class="status-select-wrapper">
            <select
              class="status-select"
              :class="interview.status"
              :value="interview.status"
              @change="handleStatusChange(($event.target as HTMLSelectElement).value)"
            >
              <option v-for="(label, key) in statusOptions" :key="key" :value="key">
                {{ label }}
              </option>
            </select>
          </div>
        </div>
        <h1 class="company-name">{{ interview.companyName }}</h1>
        <p class="position">{{ interview.position }}</p>
        <div class="meta-info">
          <span>面试时间：{{ formatDateTime(interview.interviewDate) }}</span>
          <span v-if="interview.roundType">
            面试轮次：{{ getRoundTypeLabel(interview.roundType) }}
          </span>
          <span class="result-wrapper">
            最终结果：
            <select
              class="result-select"
              :value="interview.overallResult || ''"
              @change="handleResultChange(($event.target as HTMLSelectElement).value)"
            >
              <option value="">未设置</option>
              <option v-for="(label, key) in resultOptions" :key="key" :value="key">
                {{ label }}
              </option>
            </select>
          </span>
        </div>
      </section>

      <section class="preparations-section">
        <div class="section-header">
          <h2>准备清单</h2>
          <button class="btn btn-sm" @click="showAddPreparationDialog = true">+ 添加事项</button>
        </div>
        <div class="preparations-list">
          <div
            v-for="prep in interview.preparations"
            :key="prep.id"
            class="preparation-item"
            :class="{ completed: prep.completed }"
          >
            <input
              type="checkbox"
              :checked="prep.completed"
              @change="togglePreparation(prep.id)"
            />
            <span class="prep-title">{{ prep.title }}</span>
            <button class="delete-btn" @click="handleDeletePreparation(prep.id)">×</button>
          </div>
          <div v-if="interview.preparations?.length === 0" class="empty-preparations">
            <p>暂无准备事项</p>
          </div>
        </div>
      </section>

      <section class="review-section">
        <div class="section-header">
          <h2>复盘笔记</h2>
        </div>
        <div v-if="interview.reviewNote" class="review-note">
          <div class="note-section">
            <h4>整体感受</h4>
            <p>{{ interview.reviewNote.overallFeeling || '暂无' }}</p>
          </div>
          <div class="note-section">
            <h4>好的方面</h4>
            <p>{{ interview.reviewNote.highPoints || '暂无' }}</p>
          </div>
          <div class="note-section">
            <h4>不足之处</h4>
            <p>{{ interview.reviewNote.weakPoints || '暂无' }}</p>
          </div>
          <div class="note-section">
            <h4>经验教训</h4>
            <p>{{ interview.reviewNote.lessonsLearned || '暂无' }}</p>
          </div>
        </div>
        <div v-else class="empty-review">
          <button class="btn btn-primary" @click="showReviewDialog = true">
            添加复盘笔记
          </button>
        </div>
      </section>
    </div>
  </div>

  <div v-else class="loading-state">
    <p>加载中...</p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getInterviewDetail,
  togglePreparationComplete,
  deletePreparation as deletePreparationApi,
  deleteInterview,
  updateInterview
} from '@/api/interview-center'
import {
  INTERVIEW_STATUS_LABELS,
  INTERVIEW_RESULT_LABELS,
  ROUND_TYPE_LABELS,
  type InterviewDetail,
  type InterviewStatus,
  type InterviewResult
} from '@/types/interview-center'
// 弹窗组件
import AddPreparationDialog from '@/components/interview-center/AddPreparationDialog.vue'
import ReviewNoteDialog from '@/components/interview-center/ReviewNoteDialog.vue'
import EditInterviewDialog from '@/components/interview-center/EditInterviewDialog.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const interview = ref<InterviewDetail | null>(null)
const showAddPreparationDialog = ref(false)
const showReviewDialog = ref(false)
const showEditDialog = ref(false)

const statusOptions = INTERVIEW_STATUS_LABELS
const resultOptions = INTERVIEW_RESULT_LABELS

function goBack() {
  router.push('/interview-center')
}

function getRoundTypeLabel(type: string): string {
  return ROUND_TYPE_LABELS[type as keyof typeof ROUND_TYPE_LABELS] || type
}

function formatDateTime(dateStr: string): string {
  return new Date(dateStr).toLocaleString('zh-CN')
}

async function togglePreparation(preparationId: string) {
  if (!interview.value) return
  try {
    await togglePreparationComplete(interview.value.id, preparationId)
    loadDetail()
  } catch (error) {
    console.error('切换准备事项状态失败:', error)
  }
}

async function handleDeletePreparation(preparationId: string) {
  if (!interview.value) return
  try {
    await deletePreparationApi(interview.value.id, preparationId)
    loadDetail()
  } catch (error) {
    console.error('删除准备事项失败:', error)
  }
}

async function handleDelete() {
  if (!interview.value) return
  if (!confirm('确定要删除这个面试吗？此操作不可撤销。')) return
  try {
    await deleteInterview(interview.value.id)
    router.push('/interview-center')
  } catch (error) {
    console.error('删除面试失败:', error)
  }
}

async function handleStatusChange(newStatus: string) {
  if (!interview.value || interview.value.status === newStatus) return
  try {
    await updateInterview(interview.value.id, { status: newStatus as InterviewStatus })
    interview.value.status = newStatus as InterviewStatus
  } catch (error) {
    console.error('更新面试状态失败:', error)
    alert('更新状态失败，请稍后重试')
  }
}

async function handleResultChange(newResult: string) {
  if (!interview.value) return
  try {
    const result = newResult ? (newResult as InterviewResult) : undefined
    await updateInterview(interview.value.id, { overallResult: result })
    interview.value.overallResult = result
  } catch (error) {
    console.error('更新面试结果失败:', error)
    alert('更新结果失败，请稍后重试')
  }
}

async function loadDetail() {
  const id = route.params.id as string
  if (!id) return
  loading.value = true
  try {
    interview.value = await getInterviewDetail(id)
  } catch (error) {
    console.error('加载面试详情失败:', error)
  } finally {
    loading.value = false
  }
}

// 弹窗事件处理
function handlePreparationAdded() {
  loadDetail()
}

function handleReviewSaved() {
  loadDetail()
}

function handleInterviewUpdated() {
  loadDetail()
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped lang="scss">
.interview-detail-page {
  padding: $spacing-xl;
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-xl;
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

.basic-info {
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  padding: $spacing-xl;
  margin-bottom: $spacing-xl;
}

.info-header {
  display: flex;
  gap: $spacing-sm;
  margin-bottom: $spacing-md;
}

.source-badge, .status-badge {
  font-size: 0.75rem;
  padding: 4px 12px;
  border-radius: $radius-full;
}

.source-badge.real {
  background: rgba($color-accent, 0.2);
  color: $color-accent;
}

.source-badge.mock {
  background: rgba($color-info, 0.2);
  color: $color-info;
}

.status-badge {
  background: $color-bg-tertiary;
  color: $color-text-secondary;
}

.status-select-wrapper {
  position: relative;
}

.status-select {
  appearance: none;
  background: transparent;
  border: none;
  font-size: 0.75rem;
  padding: 4px 24px 4px 12px;
  border-radius: $radius-full;
  cursor: pointer;
  color: inherit;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='10' viewBox='0 0 10 10'%3E%3Cpath fill='%23a1a1aa' d='M5 7L1 3h8z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
  transition: background-color 0.2s;

  &:hover {
    background-color: rgba($color-text-tertiary, 0.1);
  }

  &:focus {
    outline: none;
  }

  &.preparing {
    background-color: rgba($color-warning, 0.15);
    color: $color-warning;
  }

  &.in_progress {
    background-color: rgba($color-info, 0.15);
    color: $color-info;
  }

  &.completed {
    background-color: rgba($color-success, 0.15);
    color: $color-success;
  }

  &.cancelled {
    background-color: rgba($color-text-tertiary, 0.15);
    color: $color-text-tertiary;
  }

  option {
    background: $color-bg-secondary;
    color: $color-text-primary;
  }
}

.result-wrapper {
  display: inline-flex;
  align-items: center;
  gap: $spacing-xs;
}

.result-select {
  appearance: none;
  background: transparent;
  border: 1px solid $color-bg-tertiary;
  font-size: 0.875rem;
  padding: 4px 28px 4px 8px;
  border-radius: $radius-sm;
  cursor: pointer;
  color: $color-text-secondary;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='10' viewBox='0 0 10 10'%3E%3Cpath fill='%23a1a1aa' d='M5 7L1 3h8z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
  transition: border-color 0.2s;

  &:hover {
    border-color: $color-accent;
  }

  &:focus {
    outline: none;
    border-color: $color-accent;
  }

  option {
    background: $color-bg-secondary;
    color: $color-text-primary;
  }
}

.company-name {
  font-size: 1.75rem;
  font-weight: 700;
  color: $color-text-primary;
  margin-bottom: $spacing-sm;
}

.position {
  color: $color-text-secondary;
  margin-bottom: $spacing-md;
}

.meta-info {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-lg;
  color: $color-text-tertiary;
  font-size: 0.875rem;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-lg;

  h2 {
    font-size: 1.25rem;
    font-weight: 600;
    color: $color-text-primary;
  }
}

.preparations-section, .review-section {
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  padding: $spacing-xl;
  margin-bottom: $spacing-xl;
}

.preparations-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.preparation-item {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  padding: $spacing-md;
  background: $color-bg-tertiary;
  border-radius: $radius-md;

  &.completed {
    opacity: 0.6;

    .prep-title {
      text-decoration: line-through;
    }
  }

  input[type="checkbox"] {
    width: 18px;
    height: 18px;
    accent-color: $color-accent;
  }

  .prep-title {
    flex: 1;
    color: $color-text-primary;
  }

  .delete-btn {
    background: none;
    border: none;
    color: $color-text-tertiary;
    cursor: pointer;
    font-size: 1.25rem;

    &:hover {
      color: $color-error;
    }
  }
}

.review-note {
  .note-section {
    margin-bottom: $spacing-lg;

    h4 {
      font-size: 0.875rem;
      font-weight: 600;
      color: $color-text-secondary;
      margin-bottom: $spacing-sm;
    }

    p {
      color: $color-text-primary;
      line-height: 1.6;
    }
  }
}

.empty-preparations, .empty-review {
  text-align: center;
  padding: $spacing-xl;
  color: $color-text-tertiary;
}

.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  color: $color-text-tertiary;
}
</style>
