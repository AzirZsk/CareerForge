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
        <div class="status-badge-wrapper" ref="statusMenuRef">
          <span class="status-badge clickable" :class="statusClass" @click="toggleStatusMenu">
            <span class="status-dot"></span>
            {{ statusLabel }}
            <font-awesome-icon icon="fa-solid fa-chevron-down" class="dropdown-icon" />
          </span>
          <div v-if="showStatusMenu" class="status-dropdown">
            <button
              v-for="(label, key) in statusOptions"
              :key="key"
              :class="['status-option', { active: interview.status === key }]"
              @click="handleStatusChange(key as InterviewStatus)"
            >
              <span class="status-dot" :class="key"></span>
              {{ label }}
            </button>
          </div>
        </div>
        <div class="result-badge-wrapper" ref="resultMenuRef">
          <span class="result-badge clickable" :class="resultClass" @click="toggleResultMenu">
            <span class="result-dot"></span>
            {{ resultLabel }}
            <font-awesome-icon icon="fa-solid fa-chevron-down" class="dropdown-icon" />
          </span>
          <div v-if="showResultMenu" class="result-dropdown">
            <button
              v-for="(label, key) in resultOptions"
              :key="key"
              :class="['result-option', { active: effectiveResult === key }]"
              @click="handleResultChange(key as InterviewResult)"
            >
              <span class="result-dot" :class="key"></span>
              {{ label }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 第二行：信息块 -->
    <div class="header-row info-row">
      <div class="info-block">
        <font-awesome-icon icon="fa-solid fa-calendar" class="info-icon" />
        <div class="info-content">
          <span class="info-label">时间</span>
          <span class="info-value">{{ formattedDate }}</span>
        </div>
      </div>
      <div class="info-block">
        <font-awesome-icon icon="fa-solid fa-laptop" class="info-icon" />
        <div class="info-content">
          <span class="info-label">类型</span>
          <span class="info-value">{{ interviewTypeLabel }}</span>
        </div>
      </div>
      <div class="info-block">
        <font-awesome-icon icon="fa-solid fa-bullseye" class="info-icon" />
        <div class="info-content">
          <span class="info-label">轮次</span>
          <span class="info-value">{{ roundLabel }}</span>
        </div>
      </div>
      <div class="info-block" :class="timeUrgency.class">
        <font-awesome-icon icon="fa-solid fa-stopwatch" class="info-icon" />
        <div class="info-content">
          <span class="info-label">倒计时</span>
          <span class="info-value">{{ timeUrgency.text }}</span>
        </div>
      </div>
    </div>

    <!-- 第三行：面试地点/链接 -->
    <div v-if="interview.interviewType === 'onsite' && interview.location" class="detail-bar">
      <font-awesome-icon icon="fa-solid fa-location-dot" class="detail-icon" />
      <span class="detail-label">面试地点:</span>
      <span class="detail-value">{{ interview.location }}</span>
      <button class="detail-action-btn" @click="copyLocation">
        {{ locationCopied ? '已复制' : '复制' }}
      </button>
    </div>
    <MeetingLinkBar
      v-else-if="interview.interviewType === 'online'"
      :link="interview.onlineLink"
      :password="interview.meetingPassword"
    />

    <!-- 第四行：操作按钮 -->
    <div class="header-row actions-row">
      <a
        v-if="interview.resumeId"
        :href="`/resume/${interview.resumeId}`"
        target="_blank"
        rel="noopener noreferrer"
        class="resume-link-inline"
      >
        <font-awesome-icon icon="fa-solid fa-file-alt" />
        关联简历
      </a>
      <div v-else class="actions-spacer"></div>
      <div class="actions-group">
        <button
          class="btn btn-position"
          v-if="jdContent"
          @click="$emit('toggle-position')"
        >
          <font-awesome-icon icon="fa-solid fa-clipboard-list" />
          {{ showPosition ? '收起职位' : '职位信息' }}
        </button>
        <button
          class="btn btn-mock"
          :disabled="!canStartMockInterview"
          :title="mockInterviewHint"
          @click="$emit('start-mock')"
        >
          <font-awesome-icon icon="fa-solid fa-microphone" />
          开始模拟面试
        </button>
        <div class="more-menu" ref="moreMenuRef">
          <button class="btn btn-icon" @click="toggleMoreMenu">
            <font-awesome-icon icon="fa-solid fa-ellipsis-vertical" />
          </button>
          <div v-if="showMoreMenu" class="dropdown">
            <button @click="handleEdit">编辑面试</button>
            <button class="danger" @click="handleDelete">删除面试</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 职位详情展开区域 -->
    <div v-if="showPosition && jdContent" class="position-detail-content">
      <!-- Tab 切换栏 -->
      <div class="position-tabs">
        <button
          class="position-tab-btn"
          :class="{ active: activeTab === 'jd' }"
          @click="activeTab = 'jd'"
        >
          <font-awesome-icon icon="fa-solid fa-clipboard-list" /> 职位描述
        </button>
        <button
          class="position-tab-btn"
          :class="{ active: activeTab === 'research' }"
          @click="activeTab = 'research'"
        >
          <font-awesome-icon icon="fa-solid fa-building" /> 公司调研
        </button>
        <button
          class="position-tab-btn"
          :class="{ active: activeTab === 'analysis' }"
          @click="activeTab = 'analysis'"
        >
          <font-awesome-icon icon="fa-solid fa-magnifying-glass" /> JD 分析
        </button>
      </div>

      <!-- Tab 内容区域 -->
      <div class="position-tab-content">
        <!-- JD 原文 -->
        <div v-show="activeTab === 'jd'" class="tab-panel">
          <div class="jd-content">{{ jdContent }}</div>
        </div>

        <!-- 公司调研 -->
        <div v-show="activeTab === 'research'" class="tab-panel">
          <slot name="company-research"></slot>
          <div v-if="!$slots['company-research']" class="empty-state with-action">
            <div class="empty-icon">
              <font-awesome-icon icon="fa-solid fa-building" />
            </div>
            <p class="empty-title">暂无公司调研信息</p>
            <p class="empty-hint">生成面试准备清单后，系统将自动调研目标公司并展示在此处</p>
          </div>
        </div>

        <!-- JD 分析 -->
        <div v-show="activeTab === 'analysis'" class="tab-panel">
          <slot name="jd-analysis"></slot>
          <div v-if="!$slots['jd-analysis']" class="empty-state with-action">
            <div class="empty-icon">
              <font-awesome-icon icon="fa-solid fa-magnifying-glass" />
            </div>
            <p class="empty-title">暂无 JD 分析信息</p>
            <p class="empty-hint">生成面试准备清单后，系统将自动分析职位要求并展示在此处</p>
          </div>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import MeetingLinkBar from './MeetingLinkBar.vue'
import { useToast } from '@/composables/useToast'
import {
  INTERVIEW_STATUS_LABELS,
  INTERVIEW_RESULT_LABELS,
  ROUND_TYPE_LABELS,
  INTERVIEW_TYPE_LABELS,
  type InterviewDetail,
  type InterviewStatus,
  type InterviewResult
} from '@/types/interview-center'

const props = defineProps<{
  interview: InterviewDetail
  canStartMockInterview: boolean
  mockInterviewHint: string
  showPosition?: boolean
  jdContent?: string
}>()

const emit = defineEmits<{
  back: []
  'start-mock': []
  edit: []
  delete: []
  'toggle-position': []
  'status-change': [status: InterviewStatus]
  'result-change': [result: InterviewResult]
}>()

const showMoreMenu = ref(false)
const moreMenuRef = ref<HTMLElement | null>(null)
const activeTab = ref<'jd' | 'research' | 'analysis'>('jd')
const showStatusMenu = ref(false)
const showResultMenu = ref(false)
const statusMenuRef = ref<HTMLElement | null>(null)
const resultMenuRef = ref<HTMLElement | null>(null)
const locationCopied = ref(false)

const toast = useToast()

async function copyLocation() {
  if (!props.interview.location) return
  try {
    await navigator.clipboard.writeText(props.interview.location)
    locationCopied.value = true
    toast.success('地点已复制')
    setTimeout(() => { locationCopied.value = false }, 2000)
  } catch {
    toast.error('复制失败，请手动复制')
  }
}

const statusOptions = INTERVIEW_STATUS_LABELS
const resultOptions = INTERVIEW_RESULT_LABELS

// 状态标签
const statusLabel = computed(() => {
  return INTERVIEW_STATUS_LABELS[props.interview.status] || props.interview.status
})

// 状态样式类
const statusClass = computed(() => {
  return props.interview.status
})

// 实际结果值（空时为 null，不参与下拉高亮）
const effectiveResult = computed(() => {
  return props.interview.overallResult || null
})

// 结果标签
const resultLabel = computed(() => {
  if (!effectiveResult.value) return '未设置'
  return INTERVIEW_RESULT_LABELS[effectiveResult.value] || '未设置'
})

// 结果样式类
const resultClass = computed(() => {
  return effectiveResult.value || 'pending'
})

// 轮次标签
const roundLabel = computed(() => {
  if (props.interview.roundType === 'custom' && props.interview.roundName) {
    return props.interview.roundName
  }
  return ROUND_TYPE_LABELS[props.interview.roundType as keyof typeof ROUND_TYPE_LABELS] || props.interview.roundType || ''
})

// 面试类型标签
const interviewTypeLabel = computed(() => {
  return INTERVIEW_TYPE_LABELS[props.interview.interviewType as keyof typeof INTERVIEW_TYPE_LABELS] || '未设置'
})

// 格式化日期
const formattedDate = computed(() => {
  if (!props.interview.interviewDate) return '未设置'
  const date = new Date(props.interview.interviewDate)
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
})

// 时间紧迫感
const timeUrgency = computed(() => {
  if (!props.interview.interviewDate) {
    return { text: '未设置', class: 'normal' }
  }

  const interviewDate = new Date(props.interview.interviewDate).getTime()
  const now = Date.now()
  const diff = interviewDate - now
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(hours / 24)

  // 已过期
  if (diff < 0) {
    return { text: '已过期', class: 'expired' }
  }

  // 即将开始（< 1小时）
  if (hours < 1) {
    return { text: '即将开始', class: 'critical' }
  }

  // 还有 X 小时（< 24小时）
  if (hours < 24) {
    return { text: `还有 ${hours} 小时`, class: 'urgent' }
  }

  // 明天
  if (days === 1) {
    return { text: '明天', class: 'soon' }
  }

  // 3天内
  if (days <= 3) {
    return { text: `${days} 天后`, class: 'soon' }
  }

  // 更远
  return { text: `${days} 天后`, class: 'normal' }
})

// 切换更多菜单
function toggleMoreMenu() {
  showMoreMenu.value = !showMoreMenu.value
}

// 处理编辑
function handleEdit() {
  showMoreMenu.value = false
  emit('edit')
}

// 处理删除
function handleDelete() {
  showMoreMenu.value = false
  emit('delete')
}

// 切换状态菜单
function toggleStatusMenu() {
  showStatusMenu.value = !showStatusMenu.value
  showResultMenu.value = false
}

// 切换结果菜单
function toggleResultMenu() {
  showResultMenu.value = !showResultMenu.value
  showStatusMenu.value = false
}

// 处理状态变更
function handleStatusChange(status: InterviewStatus) {
  showStatusMenu.value = false
  emit('status-change', status)
}

// 处理结果变更
function handleResultChange(result: InterviewResult) {
  showResultMenu.value = false
  emit('result-change', result)
}

// 关闭所有下拉菜单
function closeAllMenus(event: MouseEvent) {
  const target = event.target as Node
  if (statusMenuRef.value && !statusMenuRef.value.contains(target)) {
    showStatusMenu.value = false
  }
  if (resultMenuRef.value && !resultMenuRef.value.contains(target)) {
    showResultMenu.value = false
  }
  if (moreMenuRef.value && !moreMenuRef.value.contains(target)) {
    showMoreMenu.value = false
  }
}

// 点击外部关闭菜单
onMounted(() => {
  document.addEventListener('click', closeAllMenus)
})

onUnmounted(() => {
  document.removeEventListener('click', closeAllMenus)
})
</script>

<style scoped lang="scss">
.interview-header {
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  margin-bottom: $spacing-xl;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;

  & + .header-row {
    margin-top: $spacing-md;
  }
}

// 第一行：标题
.title-row {
  .header-left {
    display: flex;
    align-items: center;
    gap: $spacing-md;
    flex: 1;
    min-width: 0;
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    flex-shrink: 0;
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
}

// 状态标签
.status-badge-wrapper {
  position: relative;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: $spacing-xs;
  padding: 6px 12px;
  border-radius: $radius-full;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;

  .status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
  }

  .dropdown-icon {
    font-size: 0.625rem;
    margin-left: 4px;
    opacity: 0.6;
  }

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  }

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

.status-dropdown {
  position: absolute;
  right: 0;
  top: 100%;
  margin-top: $spacing-xs;
  background: $color-bg-elevated;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-md;
  padding: $spacing-xs;
  min-width: 140px;
  z-index: 100;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);

  .status-option {
    display: flex;
    align-items: center;
    gap: $spacing-xs;
    width: 100%;
    padding: $spacing-sm $spacing-md;
    text-align: left;
    background: transparent;
    border: none;
    color: $color-text-secondary;
    border-radius: $radius-sm;
    cursor: pointer;
    font-size: 0.875rem;
    transition: all 0.2s;

    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;

      &.preparing { background: $color-warning; }
      &.in_progress { background: $color-info; }
      &.completed { background: $color-success; }
      &.cancelled { background: $color-text-tertiary; }
    }

    &:hover {
      background: $color-bg-tertiary;
      color: $color-text-primary;
    }

    &.active {
      background: rgba($color-accent, 0.15);
      color: $color-accent;
      font-weight: 500;
    }
  }
}

// 结果标签
.result-badge-wrapper {
  position: relative;
}

.result-badge {
  display: inline-flex;
  align-items: center;
  gap: $spacing-xs;
  padding: 6px 12px;
  border-radius: $radius-full;
  font-size: 0.875rem;
  font-weight: 500;
  margin-left: $spacing-sm;
  cursor: pointer;
  transition: all 0.2s;

  .result-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
  }

  .dropdown-icon {
    font-size: 0.625rem;
    margin-left: 4px;
    opacity: 0.6;
  }

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  }

  &.passed {
    background: rgba($color-success, 0.15);
    color: $color-success;
    .result-dot { background: $color-success; }
  }

  &.failed {
    background: rgba($color-error, 0.15);
    color: $color-error;
    .result-dot { background: $color-error; }
  }

  &.pending {
    background: rgba($color-text-tertiary, 0.15);
    color: $color-text-tertiary;
    .result-dot { background: $color-text-tertiary; }
  }

  &.add-result {
    background: rgba($color-accent, 0.1);
    color: $color-accent;
    border: 1px dashed rgba($color-accent, 0.3);
    font-weight: 400;

    &:hover {
      background: rgba($color-accent, 0.15);
      border-color: rgba($color-accent, 0.5);
    }
  }
}

.result-dropdown {
  position: absolute;
  right: 0;
  top: 100%;
  margin-top: $spacing-xs;
  background: $color-bg-elevated;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-md;
  padding: $spacing-xs;
  min-width: 140px;
  z-index: 100;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);

  .result-option {
    display: flex;
    align-items: center;
    gap: $spacing-xs;
    width: 100%;
    padding: $spacing-sm $spacing-md;
    text-align: left;
    background: transparent;
    border: none;
    color: $color-text-secondary;
    border-radius: $radius-sm;
    cursor: pointer;
    font-size: 0.875rem;
    transition: all 0.2s;

    .result-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;

      &.passed { background: $color-success; }
      &.failed { background: $color-error; }
      &.pending { background: $color-text-tertiary; }
    }

    &:hover {
      background: $color-bg-tertiary;
      color: $color-text-primary;
    }

    &.active {
      background: rgba($color-accent, 0.15);
      color: $color-accent;
      font-weight: 500;
    }
  }
}

// 第二行：信息块
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
  height: 56px;
  background: $color-bg-tertiary;
  border-radius: $radius-md;
  overflow: hidden;

  .info-icon {
    font-size: 1.25rem;
    color: $color-text-secondary;
  }

  .info-content {
    display: flex;
    flex-direction: column;
    gap: 2px;
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
    background: rgba($color-warning, 0.1);
    .info-value { color: $color-warning; }
  }

  &.soon {
    background: rgba($color-info, 0.1);
    .info-value { color: $color-info; }
  }

  &.expired {
    .info-value {
      color: $color-text-tertiary;
      text-decoration: line-through;
    }
  }

}

// 面试详情栏（地点等）
.detail-bar {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-md;
  background: $color-bg-tertiary;
  border-radius: $radius-md;
  margin-top: $spacing-sm;
  margin-bottom: $spacing-sm;

  .detail-icon {
    font-size: 1rem;
    color: $color-text-secondary;
  }

  .detail-label {
    font-size: 0.875rem;
    color: $color-text-tertiary;
    white-space: nowrap;
  }

  .detail-value {
    flex: 1;
    font-size: 0.875rem;
    color: $color-text-primary;
    min-width: 0;
  }

  .detail-action-btn {
    padding: 3px 8px;
    font-size: 0.7rem;
    background: transparent;
    border: none;
    border-radius: $radius-sm;
    color: $color-text-tertiary;
    cursor: pointer;
    transition: color 0.2s;
    white-space: nowrap;

    &:hover {
      color: $color-text-primary;
    }
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

// 响应式
.actions-row {
  .actions-spacer {
    flex: 1;
  }

  .resume-link-inline {
    display: flex;
    align-items: center;
    gap: $spacing-xs;
    color: $color-text-secondary;
    font-size: 0.875rem;
    text-decoration: none;
    padding: $spacing-xs 0;
    transition: color 0.2s;
    flex: 1;

    svg {
      font-size: 0.875rem;
    }

    &:hover {
      color: $color-accent;
    }
  }

  .actions-group {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
  }
}

.btn-mock {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  background: linear-gradient(135deg, #4ade80 0%, #22c55e 100%);
  color: #000;
  font-weight: 600;
  padding: $spacing-sm $spacing-lg;
  border-radius: $radius-md;
  border: none;
  cursor: pointer;
  transition: all 0.2s;

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

.btn-position {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  background: rgba($color-accent, 0.1);
  color: $color-accent;
  font-weight: 500;
  padding: $spacing-sm $spacing-md;
  border-radius: $radius-md;
  border: 1px solid rgba($color-accent, 0.2);
  cursor: pointer;
  transition: all 0.2s;

  svg {
    color: $color-accent;
  }

  &:hover {
    background: rgba($color-accent, 0.15);
    border-color: rgba($color-accent, 0.3);

    svg {
      color: $color-accent-light;
    }
  }
}

.btn-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  min-width: 38px;
  padding: 0;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: $color-text-secondary;
  cursor: pointer;
  border-radius: $radius-sm;
  transition: all 0.2s;
  font-size: 1rem;

  svg {
    color: $color-text-secondary;
  }

  &:hover {
    background: $color-bg-tertiary;
    color: $color-text-primary;
    border-color: rgba(255, 255, 255, 0.2);

    svg {
      color: $color-text-primary;
    }
  }
}

// 更多菜单
.more-menu {
  position: relative;

  .dropdown {
    position: absolute;
    right: 0;
    top: 100%;
    margin-top: $spacing-xs;
    background: $color-bg-elevated;
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: $radius-md;
    padding: $spacing-xs;
    min-width: 120px;
    z-index: 100;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);

    button {
      display: block;
      width: 100%;
      padding: $spacing-sm $spacing-md;
      text-align: left;
      background: transparent;
      border: none;
      color: $color-text-secondary;
      border-radius: $radius-sm;
      cursor: pointer;
      font-size: 0.875rem;
      transition: all 0.2s;

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
  }
}

// 响应式
@media (max-width: 768px) {
  .info-row {
    flex-wrap: wrap;
  }

  .info-block {
    flex: 1 1 calc(50% - $spacing-sm);
    min-width: 140px;
  }
}

@media (max-width: 480px) {
  .info-row {
    flex-direction: column;
  }

  .info-block {
    flex: none;
    width: 100%;
  }

  .actions-row {
    flex-direction: column;
    gap: $spacing-sm;

    .actions-spacer {
      display: none;
    }

    .actions-group {
      width: 100%;
      justify-content: space-between;
    }
  }

  .btn-mock {
    flex: 1;
    justify-content: center;
  }
}

// 职位详情展开区域样式
.position-detail-content {
  margin-top: $spacing-md;
  padding-top: $spacing-md;
  border-top: 1px solid $color-bg-elevated;
}

.position-tabs {
  display: flex;
  gap: $spacing-xs;
  margin-bottom: $spacing-sm;
  border-bottom: 1px solid $color-bg-elevated;
  padding-bottom: $spacing-xs;
}

.position-tab-btn {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-xs $spacing-sm;
  background: transparent;
  border: none;
  color: $color-text-tertiary;
  font-size: 0.8125rem;
  cursor: pointer;
  border-radius: $radius-xs $radius-xs 0 0;
  transition: all 0.2s;
  position: relative;

  &:hover {
    color: $color-text-secondary;
    background: rgba($color-accent, 0.05);
  }

  &.active {
    color: $color-accent;
    font-weight: 500;

    &::after {
      content: '';
      position: absolute;
      bottom: -$spacing-xs;
      left: 0;
      right: 0;
      height: 2px;
      background: $color-accent;
      border-radius: $radius-xs;
    }
  }
}

.position-tab-content {
  .tab-panel {
    animation: fadeIn 0.2s ease-in;
    max-height: 400px;
    overflow-y: auto;
  }

  .jd-content {
    white-space: pre-wrap;
    line-height: 1.6;
    font-size: 0.875rem;
    color: $color-text-secondary;
  }

  .empty-state {
    text-align: center;
    padding: $spacing-xl $spacing-lg;
    color: $color-text-tertiary;

    &.with-action {
      .empty-icon {
        font-size: 2rem;
        margin-bottom: $spacing-sm;
        opacity: 0.5;
      }

      .empty-title {
        font-size: 0.875rem;
        font-weight: 500;
        color: $color-text-secondary;
        margin-bottom: $spacing-xs;
      }

      .empty-hint {
        font-size: 0.75rem;
        color: $color-text-tertiary;
      }
    }
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
