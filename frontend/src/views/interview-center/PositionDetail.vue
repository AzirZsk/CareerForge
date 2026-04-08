<template>
  <div class="position-detail-page" v-if="!loading && position">
    <header class="page-header">
      <button class="back-btn" @click="goBack">
          <font-awesome-icon icon="fa-solid fa-arrow-left" />
          返回
        </button>
      <div class="header-actions">
        <button class="btn btn-primary" @click="showCreateDialog = true">
          <span class="icon">+</span>
          新建面试
        </button>
        <button class="btn btn-secondary" @click="showEditDialog = true" title="编辑职位">
          <font-awesome-icon icon="fa-solid fa-pen-to-square" />
        </button>
        <button class="btn btn-danger" @click="handleDeletePosition" title="删除职位">
          <font-awesome-icon icon="fa-solid fa-trash" />
        </button>
      </div>
    </header>

    <section class="position-info">
      <div class="info-header">
        <div class="info-main">
          <h1 class="company-name">{{ position.companyName }}</h1>
          <p class="position-title">{{ position.title }}</p>
          <div class="meta-info">
            <span>创建时间：{{ formatDate(position.createdAt) }}</span>
            <span v-if="position.interviews.length > 0">
              共 {{ position.interviews.length }} 次面试
            </span>
          </div>
        </div>
        <div class="jd-content" v-if="position.jdContent">
          <div class="jd-label">职位描述</div>
          <pre>{{ position.jdContent }}</pre>
        </div>
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
            <!-- 第一行：轮次 + 日期 -->
            <div class="interview-row primary">
              <span v-if="getRoundDisplay(interview)" class="round-info">
                {{ getRoundDisplay(interview) }}
              </span>
              <span class="interview-date">{{ formatDate(interview.date) }}</span>
            </div>
            <!-- 第二行：面试类型 + 地点/链接 -->
            <div class="interview-row secondary" v-if="interview.interviewType">
              <span class="type-badge" :class="interview.interviewType">
                {{ getInterviewTypeLabel(interview.interviewType) }}
              </span>
              <span class="location-info" v-if="interview.interviewType === 'onsite' && interview.location">
                <font-awesome-icon icon="fa-solid fa-location-dot" class="info-icon" />
                {{ interview.location }}
              </span>
              <span class="link-info" v-if="interview.interviewType === 'online' && interview.onlineLink" @click.stop>
                <font-awesome-icon icon="fa-solid fa-link" class="info-icon" />
                <a :href="interview.onlineLink" target="_blank" class="online-link">进入会议</a>
              </span>
            </div>
            <!-- 第三行：状态 + 结果 -->
            <div class="interview-row tertiary">
              <span class="status-badge" :class="interview.status">
                {{ getStatusLabel(interview.status) }}
              </span>
              <span v-if="interview.overallResult" class="result-badge" :class="interview.overallResult">
                {{ getResultLabel(interview.overallResult) }}
              </span>
            </div>
          </div>
          <div class="interview-actions">
            <button class="delete-btn" @click.stop="handleDeleteInterview(interview)" title="删除">
              <font-awesome-icon icon="fa-solid fa-trash" />
            </button>
            <span class="interview-arrow">
              <font-awesome-icon icon="fa-solid fa-chevron-right" />
            </span>
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
      :preselected-position-id="position?.id ?? null"
      @close="handleCloseCreateDialog"
      @created="handleInterviewCreated"
    />

    <EditPositionDialog
      v-if="showEditDialog && position"
      :position="position"
      @close="handleCloseEditDialog"
      @updated="handlePositionUpdated"
    />

    <ConfirmModal
      :visible="confirmVisible"
      :title="confirmTitle"
      :message="confirmMessage"
      :confirm-text="confirmText"
      :danger="confirmDanger"
      @confirm="handleConfirm"
      @cancel="handleCancel"
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
import {
  INTERVIEW_STATUS_LABELS,
  INTERVIEW_RESULT_LABELS,
  ROUND_TYPE_LABELS,
  INTERVIEW_TYPE_LABELS
} from '@/types/interview-center'
import type { InterviewBrief } from '@/types/job-position'
import CreateInterviewDialog from '@/components/interview-center/CreateInterviewDialog.vue'
import EditPositionDialog from '@/components/interview-center/EditPositionDialog.vue'
import { useConfirm } from '@/composables/useConfirm'
import { useToast } from '@/composables/useToast'
import ConfirmModal from '@/components/common/ConfirmModal.vue'

const { confirm, visible: confirmVisible, title: confirmTitle, message: confirmMessage, confirmText, danger: confirmDanger, handleConfirm, handleCancel } = useConfirm()
const toast = useToast()

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const position = ref<JobPositionDetail | null>(null)
const showCreateDialog = ref(false)
const showEditDialog = ref(false)

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
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function getStatusLabel(status: string): string {
  return INTERVIEW_STATUS_LABELS[status as keyof typeof INTERVIEW_STATUS_LABELS] || status
}

function getResultLabel(result: string): string {
  return INTERVIEW_RESULT_LABELS[result as keyof typeof INTERVIEW_RESULT_LABELS] || result
}

// 获取轮次显示文本
function getRoundDisplay(interview: InterviewBrief): string {
  if (interview.roundType === 'custom' && interview.roundName) {
    return interview.roundName
  }
  if (interview.roundType) {
    return ROUND_TYPE_LABELS[interview.roundType as keyof typeof ROUND_TYPE_LABELS] || interview.roundType
  }
  return ''
}

// 获取面试类型标签
function getInterviewTypeLabel(type?: string): string {
  if (!type) return ''
  return INTERVIEW_TYPE_LABELS[type as keyof typeof INTERVIEW_TYPE_LABELS] || type
}

function handleCloseCreateDialog() {
  showCreateDialog.value = false
}

function handleInterviewCreated(id: string) {
  showCreateDialog.value = false
  loadDetail()
  router.push(`/interview-center/${id}`)
}

function handleCloseEditDialog() {
  showEditDialog.value = false
}

function handlePositionUpdated(updatedPosition: JobPositionDetail) {
  showEditDialog.value = false
  position.value = updatedPosition
  toast.success('职位信息已更新')
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
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  background: transparent;
  border: 1px solid transparent;
  color: $color-text-secondary;
  cursor: pointer;
  font-size: 0.875rem;
  padding: $spacing-sm $spacing-md;
  border-radius: $radius-md;
  transition: all 0.2s;

  svg {
    transition: transform 0.2s;
  }

  &:hover {
    color: $color-text-primary;
    background: $color-bg-secondary;
    border-color: rgba(255, 255, 255, 0.08);

    svg {
      transform: translateX(-2px);
    }
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

.info-header {
  display: flex;
  gap: $spacing-2xl;
}

.info-main {
  flex: 0 0 auto;
  min-width: 280px;
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

.jd-content {
  flex: 1;
  min-width: 0;
  padding-left: $spacing-xl;
  border-left: 1px solid rgba(255, 255, 255, 0.08);

  .jd-label {
    font-size: 0.75rem;
    color: $color-text-tertiary;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    margin-bottom: $spacing-sm;
  }

  pre {
    white-space: pre-wrap;
    word-wrap: break-word;
    color: $color-text-secondary;
    font-size: 0.875rem;
    line-height: 1.6;
    margin: 0;
    max-height: 200px;
    overflow-y: auto;
  }
}

.interviews-section {
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  padding: $spacing-xl;
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
  display: flex;
  flex-direction: column;
  gap: 4px;
}

// 面试行布局
.interview-row {
  display: flex;
  align-items: center;
  gap: $spacing-sm;

  &.primary {
    margin-bottom: 2px;
  }

  &.secondary {
    font-size: 0.8125rem;
    color: $color-text-tertiary;
  }

  &.tertiary {
    margin-top: 2px;
  }
}

// 轮次信息
.round-info {
  color: $color-text-secondary;
  font-size: 0.875rem;
  font-weight: 500;
}

// 面试类型标签
.type-badge {
  font-size: 0.7rem;
  padding: 2px 8px;
  border-radius: $radius-full;

  &.onsite {
    background: rgba($color-accent, 0.15);
    color: $color-accent;
  }

  &.online {
    background: rgba($color-info, 0.15);
    color: $color-info;
  }
}

// 地点信息
.location-info {
  display: flex;
  align-items: center;
  gap: 4px;

  .info-icon {
    font-size: 0.75rem;
  }
}

// 链接信息
.link-info {
  display: flex;
  align-items: center;
  gap: 4px;

  .info-icon {
    font-size: 0.75rem;
  }

  .online-link {
    color: $color-accent;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }
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
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: $radius-full;
  background: $color-bg-secondary;
  color: $color-text-tertiary;
  transition: all 0.2s;
}

.interview-item:hover .interview-arrow {
  background: rgba($color-accent, 0.15);
  color: $color-accent;
  transform: translateX(2px);
}

.interview-actions {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.btn-danger {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: $color-text-tertiary;
  padding: $spacing-xs $spacing-sm;
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  transition: all 0.2s;

  &:hover {
    color: $color-error;
    background: rgba($color-error, 0.15);
    border-color: $color-error;
  }
}

.delete-btn {
  padding: $spacing-xs $spacing-sm;
  background: transparent;
  border: none;
  color: $color-text-tertiary;
  border-radius: $radius-sm;
  cursor: pointer;
  transition: all 0.2s;
  opacity: 0;

  &:hover {
    color: $color-error;
    background: rgba($color-error, 0.1);
  }
}

.interview-item:hover .delete-btn {
  opacity: 1;
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
