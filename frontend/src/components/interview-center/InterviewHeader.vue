<template>
  <header class="interview-header">
    <!-- 第一行：标题 + 状态标签 -->
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
        <span class="status-badge" :class="statusClass">
          <span class="status-dot"></span>
          {{ statusLabel }}
        </span>
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

    <!-- 第三行：关联简历 -->
    <div class="header-row resume-row" v-if="interview.resumeId">
      <span class="resume-icon">📄</span>
      <span class="resume-label">关联简历：</span>
      <router-link :to="`/resume/${interview.resumeId}`" class="resume-link">
        {{ interview.resumeName || '查看简历' }}
      </router-link>
    </div>

    <!-- 第四行：会议链接（仅线上面试） -->
    <MeetingLinkBar
      v-if="interview.interviewType === 'online'"
      :link="interview.onlineLink"
      :password="interview.meetingPassword"
    />

    <!-- 第五行：操作按钮 -->
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
        <div class="more-menu" ref="moreMenuRef">
          <button class="btn btn-icon" @click="toggleMoreMenu">
            <font-awesome-icon icon="fa-solid fa-ellipsis-vertical" />
          </button>
          <div v-if="showMoreMenu" class="dropdown">
            <button @click="handleEdit">编辑面试</button>
            <button @click="handleEditResume">关联简历</button>
            <button class="danger" @click="handleDelete">删除面试</button>
          </div>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import MeetingLinkBar from './MeetingLinkBar.vue'
import {
  INTERVIEW_STATUS_LABELS,
  ROUND_TYPE_LABELS,
  INTERVIEW_TYPE_LABELS,
  type InterviewDetail
} from '@/types/interview-center'

const props = defineProps<{
  interview: InterviewDetail
  canStartMockInterview: boolean
  mockInterviewHint: string
}>()

const emit = defineEmits<{
  back: []
  'start-mock': []
  edit: []
  delete: []
  'edit-resume': []
}>()

const showMoreMenu = ref(false)
const moreMenuRef = ref<HTMLElement | null>(null)

// 状态标签
const statusLabel = computed(() => {
  return INTERVIEW_STATUS_LABELS[props.interview.status] || props.interview.status
})

// 状态样式类
const statusClass = computed(() => {
  return props.interview.status
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
  const date = new Date(props.interview.interviewDate)
  return {
    text: date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' }),
    class: 'normal'
  }
})

// 切换更多菜单
function toggleMoreMenu() {
  showMoreMenu.value = !showMoreMenu.value
}

// 关闭更多菜单
function closeMoreMenu(event: MouseEvent) {
  if (moreMenuRef.value && !moreMenuRef.value.contains(event.target as Node)) {
    showMoreMenu.value = false
  }
}

// 处理编辑
function handleEdit() {
  showMoreMenu.value = false
  emit('edit')
}

// 处理关联简历
function handleEditResume() {
  showMoreMenu.value = false
  emit('edit-resume')
}

// 处理删除
function handleDelete() {
  showMoreMenu.value = false
  emit('delete')
}

// 点击外部关闭菜单
onMounted(() => {
  document.addEventListener('click', closeMoreMenu)
})

onUnmounted(() => {
  document.removeEventListener('click', closeMoreMenu)
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
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: $spacing-xs;
  padding: 6px 12px;
  border-radius: $radius-full;
  font-size: 0.875rem;
  font-weight: 500;

  .status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
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
  background: $color-bg-tertiary;
  border-radius: $radius-md;

  .info-icon {
    font-size: 1.25rem;
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

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

// 第三行：关联简历
.resume-row {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-md;
  background: $color-bg-tertiary;
  border-radius: $radius-md;

  .resume-icon {
    font-size: 1rem;
  }

  .resume-label {
    font-size: 0.875rem;
    color: $color-text-tertiary;
  }

  .resume-link {
    color: $color-accent;
    text-decoration: none;
    font-size: 0.875rem;
    font-weight: 500;

    &:hover {
      text-decoration: underline;
    }
  }
}

// 第五行：操作按钮
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

.btn-icon {
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

  &:hover {
    color: $color-text-primary;
    background: $color-bg-tertiary;
    border-color: rgba(255, 255, 255, 0.1);
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
</style>
