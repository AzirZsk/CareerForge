<template>
  <div class="position-detail-page" v-if="!loading && position">
    <header class="page-header">
      <button class="back-btn" @click="goBack">← 返回</button>
      <div class="header-actions">
        <button class="btn btn-primary" @click="showCreateDialog = true">
          <span class="icon">+</span>
          新建面试
        </button>
        <button class="btn btn-danger" @click="handleDeletePosition">
          删除职位
        </button>
      </div>
    </header>

    <section class="position-info">
      <h1 class="company-name">{{ position.companyName }}</h1>
      <p class="position-title">{{ position.title }}</p>
      <div class="meta-info">
        <span>创建时间：{{ formatDate(position.createdAt) }}</span>
        <span v-if="position.interviews.length > 0">
          共 {{ position.interviews.length }} 次面试
        </span>
      </div>
    </section>

    <section class="jd-section" v-if="position.jdContent">
      <div class="section-header">
        <h2>职位描述</h2>
      </div>
      <div class="jd-content">
        <pre>{{ position.jdContent }}</pre>
      </div>
    </section>

    <section class="interviews-section">
      <div class="section-header">
        <h2>面试记录</h2>
      </div>
      <div class="interviews-list" v-if="position.interviews.length > 0">
        <div
          v-for="interview in position.interviews"
          :key="interview.id"
          class="interview-item"
          @click="goToInterviewDetail(interview.id)"
        >
          <div class="interview-main">
            <div class="interview-header">
              <span class="interview-date">{{ formatDate(interview.date) }}</span>
              <span class="status-badge" :class="interview.status">
                {{ getStatusLabel(interview.status) }}
              </span>
              <span v-if="interview.overallResult" class="result-badge" :class="interview.overallResult">
                {{ getResultLabel(interview.overallResult) }}
              </span>
            </div>
          </div>
          <div class="interview-actions">
            <button class="delete-btn" @click.stop="handleDeleteInterview(interview)">
              删除
            </button>
            <span class="interview-arrow">→</span>
          </div>
        </div>
      </div>
      <div v-else class="empty-state">
        <p>暂无面试记录</p>
        <button class="btn btn-primary" @click="showCreateDialog = true">
          创建第一个面试
        </button>
      </div>
    </section>

    <CreateInterviewDialog
      v-if="showCreateDialog"
      :preselected-position="preselectedPosition"
      @close="handleCloseCreateDialog"
      @created="handleInterviewCreated"
    />
  </div>

  <div v-else class="loading-state">
    <p>加载中...</p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getJobPositionDetail, deleteJobPosition } from '@/api/job-position'
import { deleteInterview } from '@/api/interview-center'
import type { JobPositionDetail } from '@/types/job-position'
import { INTERVIEW_STATUS_LABELS, INTERVIEW_RESULT_LABELS } from '@/types/interview-center'
import CreateInterviewDialog from '@/components/interview-center/CreateInterviewDialog.vue'
import { useConfirm } from '@/composables/useConfirm'
import { useToast } from '@/composables/useToast'

const { confirm } = useConfirm()
const toast = useToast()

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const position = ref<JobPositionDetail | null>(null)
const showCreateDialog = ref(false)
const preselectedPosition = ref<{ companyName: string; title: string } | null>(null)

function goBack() {
  router.push('/interview-center')
}

function goToInterviewDetail(id: string) {
  router.push(`/interview-center/${id}`)
}

function formatDate(dateStr?: string): string {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

function getStatusLabel(status: string): string {
  return INTERVIEW_STATUS_LABELS[status as keyof typeof INTERVIEW_STATUS_LABELS] || status
}

function getResultLabel(result: string): string {
  return INTERVIEW_RESULT_LABELS[result as keyof typeof INTERVIEW_RESULT_LABELS] || result
}

function handleCloseCreateDialog() {
  showCreateDialog.value = false
  preselectedPosition.value = null
}

function handleInterviewCreated(id: string) {
  showCreateDialog.value = false
  preselectedPosition.value = null
  loadDetail()
  router.push(`/interview-center/${id}`)
}

async function handleDeletePosition() {
  if (!position.value) return
  const confirmed = await confirm({
    title: '删除确认',
    message: `确定要删除职位「${position.value.companyName} - ${position.value.title}」吗？删除后无法恢复，该职位下的所有面试记录也会被删除`,
    danger: true
  })
  if (!confirmed) return

  try {
    await deleteJobPosition(position.value.id)
    toast.success('职位已删除')
    router.push('/interview-center')
  } catch (error) {
    const message = error instanceof Error ? error.message : '删除失败'
    toast.error(message)
  }
}

async function handleDeleteInterview(interview: { id: string; date?: string }) {
  const confirmed = await confirm({
    title: '删除确认',
    message: `确定要删除这条面试记录吗？面试日期：${formatDate(interview.date)}，删除后无法恢复`,
    danger: true
  })
  if (!confirmed) return

  try {
    await deleteInterview(interview.id)
    toast.success('面试记录已删除')
    loadDetail()
  } catch (error) {
    const message = error instanceof Error ? error.message : '删除失败'
    toast.error(message)
  }
}

async function loadDetail() {
  const id = route.params.id as string
  if (!id) return
  loading.value = true
  try {
    position.value = await getJobPositionDetail(id)
    preselectedPosition.value = {
      companyName: position.value.companyName,
      title: position.value.title
    }
  } catch (error) {
    console.error('加载职位详情失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped lang="scss">
.position-detail-page {
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

.header-actions {
  display: flex;
  gap: $spacing-sm;
}

.position-info {
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  padding: $spacing-xl;
  margin-bottom: $spacing-xl;
}

.company-name {
  font-size: 1.75rem;
  font-weight: 700;
  color: $color-text-primary;
  margin-bottom: $spacing-sm;
}

.position-title {
  color: $color-text-secondary;
  font-size: 1.125rem;
  margin-bottom: $spacing-md;
}

.meta-info {
  display: flex;
  gap: $spacing-lg;
  color: $color-text-tertiary;
  font-size: 0.875rem;
}

.jd-section {
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  padding: $spacing-xl;
  margin-bottom: $spacing-xl;
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

.jd-content {
  pre {
    white-space: pre-wrap;
    word-wrap: break-word;
    color: $color-text-secondary;
    font-size: 0.875rem;
    line-height: 1.6;
    margin: 0;
  }
}

.interviews-section {
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  padding: $spacing-xl;
}

.interviews-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.interview-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-md $spacing-lg;
  background: $color-bg-tertiary;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: lighten($color-bg-tertiary, 5%);
  }
}

.interview-main {
  flex: 1;
}

.interview-header {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.interview-date {
  color: $color-text-primary;
  font-weight: 500;
}

.status-badge, .result-badge {
  font-size: 0.75rem;
  padding: 2px 8px;
  border-radius: $radius-sm;
}

.status-badge {
  background: $color-bg-secondary;
  color: $color-text-secondary;

  &.preparing { color: $color-info; }
  &.in_progress { color: $color-accent; }
  &.completed { color: $color-success; }
  &.cancelled { color: $color-text-tertiary; }
}

.result-badge {
  background: $color-bg-secondary;

  &.passed {
    color: $color-success;
  }
  &.failed {
    color: $color-error;
  }
  &.pending {
    color: $color-text-tertiary;
  }
}

.interview-arrow {
  color: $color-text-tertiary;
  font-size: 1.25rem;
}

.interview-actions {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.delete-btn {
  padding: $spacing-xs $spacing-sm;
  background: transparent;
  border: 1px solid $color-error;
  color: $color-error;
  border-radius: $radius-sm;
  cursor: pointer;
  font-size: 0.75rem;
  transition: all 0.2s;
  opacity: 0;
}

.interview-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background: rgba($color-error, 0.1);
}

.btn-danger {
  background: transparent;
  border: 1px solid $color-error;
  color: $color-error;

  &:hover {
    background: rgba($color-error, 0.1);
  }
}

.empty-state {
  text-align: center;
  padding: $spacing-2xl;
  color: $color-text-tertiary;

  p {
    margin-bottom: $spacing-md;
  }
}

.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  color: $color-text-tertiary;
}
</style>
