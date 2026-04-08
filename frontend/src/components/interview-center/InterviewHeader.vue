<template>
  <header class="interview-header">
    <!-- 第一行：标题 + 状态标签 + 结果标签 -->
    <div class="header-row title-row">
      <div class="header-left">
        <button class="back-btn" @click="$emit('back')">
          <font-awesome-icon icon="fa-solid fa-arrow-left" />
        </button>
        <div class="header-title">
          <span class="company-name">{{ interview.companyName }}</span>
          <span class="separator">/</span>
          <span class="position-name">{{ interview.position }}</span>
          <span v-if="interview.roundType" class="round-badge">{{ roundLabel }}</span>
        </div>
      </div>
      <div class="header-right">
        <!-- 状态标签 -->
        <div class="status-tag-wrapper">
          <button
            class="status-tag"
            :class="interview.status"
            @click="toggleStatusDropdown"
          >
            <span class="status-dot"></span>
            <span class="status-text">{{ statusLabel }}</span>
            <font-awesome-icon icon="fa-solid fa-chevron-down" class="dropdown-icon" />
          </button>
          <div v-if="showStatusDropdown" class="status-dropdown">
            <button
              v-for="(label, key) in statusOptions"
              :key="key"
              class="dropdown-item"
              :class="{ active: interview.status === key }"
              @click="handleStatusChange(key as InterviewStatus)"
            >
              <span class="item-dot" :class="key"></span>
              {{ label }}
            </button>
          </div>
        </div>
        <!-- 结果标签 -->
        <div class="status-tag-wrapper">
          <button
            class="result-tag"
            :class="resultClass"
            @click="toggleResultDropdown"
          >
            <span class="result-dot"></span>
            <span class="result-text">{{ resultLabel }}</span>
            <font-awesome-icon icon="fa-solid fa-chevron-down" class="dropdown-icon" />
          </button>
          <div v-if="showResultDropdown" class="status-dropdown">
            <button
              v-for="(label, key) in resultOptions"
              :key="key"
              class="dropdown-item"
              :class="{ active: interview.overallResult === key }"
              @click="handleResultChange(key as InterviewResult || undefined)"
            >
              <span class="item-dot" :class="key || 'pending'"></span>
              {{ label }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 第二行：四个信息块 -->
    <div class="header-row info-row">
      <div class="info-block">
        <span class="info-icon">📅</span>
        <div class="info-content">
          <span class="info-label">时间</span>
          <span class="info-value">{{ formattedDate }}</span>
        </div>
      </div>
      <div class="info-block">
        <span class="info-icon">💻</span>
        <div class="info-content">
          <span class="info-label">类型</span>
          <span class="info-value">{{ interviewTypeLabel }}</span>
        </div>
      </div>
      <div class="info-block">
        <span class="info-icon">🎯</span>
        <div class="info-content">
          <span class="info-label">轮次</span>
          <span class="info-value">{{ roundLabel }}</span>
        </div>
      </div>
      <div class="info-block" :class="timeUrgency.class">
        <span class="info-icon">⏱️</span>
        <div class="info-content">
          <span class="info-label">倒计时</span>
          <span class="info-value">{{ timeUrgency.text }}</span>
        </div>
      </div>
    </div>

    <!-- 第三行：会议链接（条件渲染） -->
    <MeetingLinkBar
      v-if="interview.interviewType === 'online' && interview.onlineLink"
      :link="interview.onlineLink"
      :password="interview.meetingPassword"
    />

    <!-- 第四行：操作按钮 -->
    <div class="header-row actions-row">
      <div class="actions-spacer"></div>
      <div class="actions-group">
        <button
          class="btn btn-mock"
          :disabled="!canStartMockInterview"
          :title="mockInterviewHint"
          @click="$emit('start-mock')"
        >
          <font-awesome-icon icon="fa-solid fa-microphone" />
          开始模拟面试
        </button>
        <div class="more-menu">
          <button class="btn btn-icon" @click="toggleMoreMenu">
            <font-awesome-icon icon="fa-solid fa-ellipsis-vertical" />
          </button>
          <div v-if="showMoreMenu" class="dropdown-menu">
            <button class="menu-item" @click="handleEdit">
              <font-awesome-icon icon="fa-solid fa-pen" />
              编辑面试
            </button>
            <button class="menu-item" v-if="interview.resumeId" @click="handleViewResume">
              <font-awesome-icon icon="fa-solid fa-file-lines" />
              查看简历
            </button>
            <div class="menu-divider"></div>
            <button class="menu-item danger" @click="handleDelete">
              <font-awesome-icon icon="fa-solid fa-trash" />
              删除面试
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 点击外部关闭下拉菜单 -->
    <div v-if="showStatusDropdown || showResultDropdown || showMoreMenu" class="click-outside" @click="closeAllDropdowns"></div>
  </header>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import MeetingLinkBar from './MeetingLinkBar.vue'
import {
  INTERVIEW_STATUS_LABELS,
  INTERVIEW_RESULT_LABELS,
  ROUND_TYPE_LABELS,
  INTERVIEW_TYPE_LABELS,
  type InterviewDetail,
  type InterviewStatus,
  type InterviewResult
} from '@/types/interview-center'

interface Props {
  interview: InterviewDetail
}

const props = defineProps<Props>()
const router = useRouter()

const emit = defineEmits<{
  (e: 'back'): void
  (e: 'edit'): void
  (e: 'delete'): void
  (e: 'start-mock'): void
  (e: 'update-status', status: InterviewStatus): void
  (e: 'update-result', result: InterviewResult | undefined): void
}>()

const statusOptions = INTERVIEW_STATUS_LABELS
const resultOptions = { '': '未设置', ...INTERVIEW_RESULT_LABELS }

const showStatusDropdown = ref(false)
const showResultDropdown = ref(false)
const showMoreMenu = ref(false)

// 计算属性
const statusLabel = computed(() => {
  return statusOptions[props.interview.status] || props.interview.status
})

const resultLabel = computed(() => {
  if (!props.interview.overallResult) return '未设置'
  return resultOptions[props.interview.overallResult] || props.interview.overallResult
})

const resultClass = computed(() => {
  return props.interview.overallResult || 'pending'
})

const roundLabel = computed(() => {
  if (props.interview.roundType === 'custom' && props.interview.roundName) {
    return props.interview.roundName
  }
  return ROUND_TYPE_LABELS[props.interview.roundType as keyof typeof ROUND_TYPE_LABELS] || props.interview.roundType || '未设置'
})

const interviewTypeLabel = computed(() => {
  if (!props.interview.interviewType) return '未设置'
  return INTERVIEW_TYPE_LABELS[props.interview.interviewType as keyof typeof INTERVIEW_TYPE_LABELS] || props.interview.interviewType || '未设置'
})

const formattedDate = computed(() => {
  if (!props.interview.interviewDate) return '未设置'
  return formatHumanDate(props.interview.interviewDate)
})

const canStartMockInterview = computed(() => {
  return !!props.interview.jobPositionId
})

const mockInterviewHint = computed(() => {
  if (canStartMockInterview.value) {
    return '基于该面试的职位 JD 进行模拟面试'
  }
  return '请先关联职位后再开始模拟面试'
})

// 时间紧迫感计算
const timeUrgency = computed(() => {
  if (!props.interview.interviewDate) {
    return { text: '未设置', class: 'normal' }
  }
  return getTimeUrgency(props.interview.interviewDate)
})

// 方法
function formatHumanDate(dateStr: string): string {
  const date = new Date(dateStr)
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const tomorrow = new Date(today)
  tomorrow.setDate(tomorrow.getDate() + 1)
  const targetDay = new Date(date.getFullYear(), date.getMonth(), date.getDate())

  const timeStr = date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })

  if (targetDay.getTime() === today.getTime()) {
    return `今天 ${timeStr}`
  }
  if (targetDay.getTime() === tomorrow.getTime()) {
    return `明天 ${timeStr}`
  }
  const diffDays = Math.floor((targetDay.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))
  if (diffDays > 0 && diffDays <= 7) {
    return `${diffDays} 天后 ${timeStr}`
  }
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' }) + ' ' + timeStr
}

function getTimeUrgency(dateStr: string): { text: string; class: string } {
  const diff = new Date(dateStr).getTime() - Date.now()
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(hours / 24)

  if (diff < 0) return { text: '已过期', class: 'expired' }
  if (hours < 1) return { text: '即将开始', class: 'critical' }
  if (hours < 24) return { text: `还有 ${hours} 小时`, class: 'urgent' }
  if (days === 1) return { text: '明天', class: 'soon' }
  if (days <= 3) return { text: `${days} 天后`, class: 'soon' }
  return { text: formatHumanDate(dateStr), class: 'normal' }
}

function toggleStatusDropdown() {
  showStatusDropdown.value = !showStatusDropdown.value
  showResultDropdown.value = false
  showMoreMenu.value = false
}

function toggleResultDropdown() {
  showResultDropdown.value = !showResultDropdown.value
  showStatusDropdown.value = false
  showMoreMenu.value = false
}

function toggleMoreMenu() {
  showMoreMenu.value = !showMoreMenu.value
  showStatusDropdown.value = false
  showResultDropdown.value = false
}

function closeAllDropdowns() {
  showStatusDropdown.value = false
  showResultDropdown.value = false
  showMoreMenu.value = false
}

function handleStatusChange(status: InterviewStatus) {
  if (status !== props.interview.status) {
    emit('update-status', status)
  }
  closeAllDropdowns()
}

function handleResultChange(result: InterviewResult | undefined) {
  if (result !== props.interview.overallResult) {
    emit('update-result', result)
  }
  closeAllDropdowns()
}

function handleEdit() {
  emit('edit')
  closeAllDropdowns()
}

function handleViewResume() {
  if (props.interview.resumeId) {
    router.push(`/resume/${props.interview.resumeId}`)
  }
  closeAllDropdowns()
}

function handleDelete() {
  emit('delete')
  closeAllDropdowns()
}

// ESC 键关闭下拉菜单
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    closeAllDropdowns()
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped lang="scss">
.interview-header {
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  margin-bottom: $spacing-xl;
  position: relative;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;

  & + .header-row {
    margin-top: $spacing-md;
  }
}

// ===== 第一行：标题 + 状态 =====
.header-left {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: transparent;
  border: 1px solid $color-bg-elevated;
  color: $color-text-secondary;
  cursor: pointer;
  border-radius: $radius-md;
  transition: all 0.2s;

  svg {
    transition: transform 0.2s;
  }

  &:hover {
    color: $color-text-primary;
    background: $color-bg-tertiary;
    border-color: rgba(255, 255, 255, 0.1);

    svg {
      transform: translateX(-2px);
    }
  }
}

.header-title {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  font-size: 1.125rem;

  .company-name {
    font-weight: 600;
    color: $color-text-primary;
  }

  .separator {
    color: $color-text-tertiary;
  }

  .position-name {
    color: $color-text-secondary;
  }

  .round-badge {
    font-size: 0.75rem;
    padding: 4px 10px;
    background: rgba($color-accent, 0.15);
    color: $color-accent;
    border-radius: $radius-full;
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

// ===== 状态标签 =====
.status-tag-wrapper {
  position: relative;
}

.status-tag,
.result-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: $radius-full;
  font-size: 0.875rem;
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: all 0.2s;

  .status-dot,
  .result-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
  }

  .dropdown-icon {
    font-size: 0.625rem;
    opacity: 0.7;
    transition: transform 0.2s;
  }

  &:hover .dropdown-icon {
    transform: translateY(2px);
  }
}

// 状态标签颜色
.status-tag {
  &.preparing {
    background: rgba($color-warning, 0.15);
    color: $color-warning;
    .status-dot { background: $color-warning; }
  }

  &.in_progress {
    background: rgba($color-info, 0.15);
    color: $color-info;
    .status-dot { background: $color-info; }
  }

  &.completed {
    background: rgba($color-success, 0.15);
    color: $color-success;
    .status-dot { background: $color-success; }
  }

  &.cancelled {
    background: rgba($color-text-tertiary, 0.15);
    color: $color-text-tertiary;
    .status-dot { background: $color-text-tertiary; }
  }
}

// 结果标签颜色
.result-tag {
  .result-dot {
    border: 2px solid currentColor;
    background: transparent;
  }

  &.pending {
    background: rgba($color-text-tertiary, 0.1);
    color: $color-text-tertiary;
  }

  &.passed {
    background: rgba($color-success, 0.15);
    color: $color-success;
    .result-dot { background: $color-success; border-color: $color-success; }
  }

  &.failed {
    background: rgba($color-error, 0.15);
    color: $color-error;
    .result-dot { background: $color-error; border-color: $color-error; }
  }
}

// 下拉菜单
.status-dropdown {
  position: absolute;
  right: 0;
  top: calc(100% + 4px);
  min-width: 120px;
  background: $color-bg-elevated;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-md;
  padding: $spacing-xs;
  z-index: $z-dropdown;
  box-shadow: $shadow-md;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  width: 100%;
  padding: $spacing-sm $spacing-md;
  font-size: 0.875rem;
  color: $color-text-secondary;
  background: transparent;
  border: none;
  border-radius: $radius-sm;
  cursor: pointer;
  transition: all 0.15s;
  text-align: left;

  &:hover {
    background: $color-bg-tertiary;
    color: $color-text-primary;
  }

  &.active {
    background: rgba($color-accent, 0.1);
    color: $color-accent;
  }

  .item-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;

    &.preparing { background: $color-warning; }
    &.in_progress { background: $color-info; }
    &.completed { background: $color-success; }
    &.cancelled { background: $color-text-tertiary; }
    &.pending { background: $color-text-tertiary; }
    &.passed { background: $color-success; }
    &.failed { background: $color-error; }
  }
}

// ===== 第二行：信息块 =====
.info-row {
  display: flex;
  gap: $spacing-md;
}

.info-block {
  flex: 1;
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-md;
  background: $color-bg-tertiary;
  border-radius: $radius-md;
  transition: all 0.2s;

  .info-icon {
    font-size: 1.25rem;
  }

  .info-content {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }

  .info-label {
    font-size: 0.75rem;
    color: $color-text-tertiary;
  }

  .info-value {
    font-size: 0.875rem;
    font-weight: 500;
    color: $color-text-primary;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  // 紧迫感样式
  &.critical {
    background: rgba($color-error, 0.15);
    .info-value { color: $color-error; }
    animation: pulse 1s infinite;
  }

  &.urgent {
    background: rgba($color-warning, 0.15);
    .info-value { color: $color-warning; }
  }

  &.soon {
    background: rgba($color-info, 0.15);
    .info-value { color: $color-info; }
  }

  &.expired {
    background: rgba($color-text-tertiary, 0.1);
    .info-value {
      color: $color-text-tertiary;
      text-decoration: line-through;
    }
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

// ===== 第四行：操作按钮 =====
.actions-row {
  .actions-spacer {
    flex: 1;
  }

  .actions-group {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
  }
}

.btn {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: 0.875rem;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-mock {
  background: linear-gradient(135deg, #4ade80 0%, #22c55e 100%);
  color: #000;
  font-weight: 600;
  padding: $spacing-sm $spacing-lg;
  border: none;

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(74, 222, 128, 0.3);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    background: $color-bg-tertiary;
    color: $color-text-tertiary;
    box-shadow: none;
  }
}

.btn-icon {
  width: 36px;
  height: 36px;
  padding: 0;
  background: transparent;
  border: 1px solid $color-bg-elevated;
  color: $color-text-secondary;
  justify-content: center;

  &:hover {
    border-color: rgba(255, 255, 255, 0.2);
    color: $color-text-primary;
    background: $color-bg-tertiary;
  }
}

// 更多菜单
.more-menu {
  position: relative;
}

.dropdown-menu {
  position: absolute;
  right: 0;
  top: calc(100% + 4px);
  min-width: 140px;
  background: $color-bg-elevated;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-md;
  padding: $spacing-xs;
  z-index: $z-dropdown;
  box-shadow: $shadow-md;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  width: 100%;
  padding: $spacing-sm $spacing-md;
  font-size: 0.875rem;
  color: $color-text-secondary;
  background: transparent;
  border: none;
  border-radius: $radius-sm;
  cursor: pointer;
  transition: all 0.15s;
  text-align: left;

  &:hover {
    background: $color-bg-tertiary;
    color: $color-text-primary;
  }

  &.danger {
    color: $color-error;

    &:hover {
      background: rgba($color-error, 0.1);
    }
  }
}

.menu-divider {
  height: 1px;
  background: $color-bg-tertiary;
  margin: $spacing-xs 0;
}

// 点击外部遮罩
.click-outside {
  position: fixed;
  inset: 0;
  z-index: $z-base;
}

// ===== 响应式 =====
@media (max-width: $breakpoint-md) {
  .info-row {
    flex-wrap: wrap;
  }

  .info-block {
    flex: 1 1 calc(50% - #{$spacing-sm});
    min-width: calc(50% - #{$spacing-sm});
  }

  .header-title {
    font-size: 1rem;
  }
}

@media (max-width: $breakpoint-sm) {
  .interview-header {
    padding: $spacing-md;
  }

  .header-row.title-row {
    flex-wrap: wrap;
    gap: $spacing-sm;
  }

  .header-left {
    flex: 1;
    min-width: 0;
  }

  .header-right {
    width: 100%;
    justify-content: flex-start;
  }

  .info-block {
    flex: 1 1 100%;
  }

  .actions-row {
    flex-direction: column;
    align-items: stretch;

    .actions-spacer {
      display: none;
    }

    .actions-group {
      justify-content: flex-end;
    }
  }
}
</style>
