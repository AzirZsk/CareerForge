<template>
  <div class="job-position-card" :class="statusClass" @click="$emit('click')">
    <div class="card-header">
      <div class="company-info">
        <h3 class="company-name">{{ jobPosition.companyName }}</h3>
        <span class="position-title">{{ jobPosition.title }}</span>
      </div>
      <div class="header-badges">
        <PositionStatusBadge v-if="jobPosition.status" :status="jobPosition.status" :editable="true" @change="handleStatusChange" />
        <span class="interview-badge" v-if="jobPosition.interviewCount > 0">
          {{ jobPosition.interviewCount }} 次面试
        </span>
      </div>
    </div>

    <!-- 下次面试高亮区域 -->
    <div class="next-interview-highlight" v-if="jobPosition.nextInterviewDate">
      <div class="next-interview-icon">
          <font-awesome-icon icon="fa-solid fa-calendar" />
        </div>
      <div class="next-interview-info">
        <span class="next-interview-time">{{ formatNextInterview(jobPosition.nextInterviewDate) }}</span>
        <span class="next-interview-round" v-if="jobPosition.nextInterviewRound">
          {{ jobPosition.nextInterviewRound }}
        </span>
      </div>
    </div>

    <div class="card-meta" v-if="jobPosition.latestInterviewDate">
      <div class="meta-item">
        <span class="meta-label">最近面试</span>
        <span class="meta-value">{{ formatDateTime(jobPosition.latestInterviewDate) }}</span>
      </div>
    </div>

    <div class="card-actions">
      <button class="action-btn" @click.stop="$emit('add-interview', jobPosition)">
        <font-awesome-icon icon="fa-solid fa-plus" />
        新建面试
      </button>
      <button class="action-btn secondary" @click.stop="$emit('view-detail', jobPosition)">
        查看详情
      </button>
      <button class="action-btn icon-btn danger" @click.stop="$emit('delete', jobPosition)" title="删除">
        <font-awesome-icon icon="fa-solid fa-trash" />
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { JobPositionListItem, PositionStatus } from '@/types/job-position'
import PositionStatusBadge from './PositionStatusBadge.vue'
import { updatePositionStatus } from '@/api/job-position'

const props = defineProps<{
  jobPosition: JobPositionListItem
}>()

defineEmits<{
  click: []
  'add-interview': [jobPosition: JobPositionListItem]
  'view-detail': [jobPosition: JobPositionListItem]
  'delete': [jobPosition: JobPositionListItem]
}>()

// 状态类名
const statusClass = computed(() => {
  return `status-${props.jobPosition.status || 'draft'}`
})

async function handleStatusChange(newStatus: PositionStatus) {
  try {
    await updatePositionStatus(props.jobPosition.id, newStatus)
    props.jobPosition.status = newStatus
  } catch (error) {
    console.error('更新职位状态失败:', error)
  }
}

// 格式化日期+时间
function formatDateTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })
}

// 格式化日期（绝对时间）
function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

// 格式化下次面试时间
function formatNextInterview(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const targetDate = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const diffDays = Math.floor((targetDate.getTime() - today.getTime()) / 86400000)
  const timeStr = date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })
  let dayStr: string
  if (diffDays === 0) {
    dayStr = '今天'
  } else if (diffDays === 1) {
    dayStr = '明天'
  } else if (diffDays === 2) {
    dayStr = '后天'
  } else if (diffDays > 2 && diffDays <= 7) {
    dayStr = `${diffDays} 天后`
  } else {
    dayStr = formatDate(dateStr)
  }
  return `${dayStr} ${timeStr}`
}
</script>

<style scoped lang="scss">
@use "sass:color";

.job-position-card {
  display: flex;
  flex-direction: column;
  min-height: 180px;
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;

  &:hover {
    border-color: $color-accent;
    transform: translateY(-2px);
  }

  // 根据状态添加左边框高亮
  &.status-interviewing {
    border-left: 3px solid #fbbf24;
  }

  &.status-offered {
    border-left: 3px solid #34d399;
  }

  &.status-rejected {
    border-left: 3px solid #f87171;
    opacity: 0.8;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: $spacing-md;
  gap: $spacing-sm;
}

.company-info {
  flex: 1;
  min-width: 0;
}

.company-name {
  font-size: 1.25rem;
  font-weight: 600;
  color: $color-text-primary;
  margin-bottom: $spacing-xs;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.position-title {
  font-size: 0.875rem;
  color: $color-text-secondary;
}

.header-badges {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  flex-shrink: 0;
}

.interview-badge {
  background: rgba($color-accent, 0.2);
  color: $color-accent;
  padding: $spacing-xs $spacing-sm;
  border-radius: $radius-sm;
  font-size: 0.75rem;
  font-weight: 500;
}

// 下次面试高亮区域
.next-interview-highlight {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-md;
  background: rgba($color-warning, 0.1);
  border: 1px solid rgba($color-warning, 0.2);
  border-radius: $radius-md;
  margin-bottom: $spacing-md;
}

.next-interview-icon {
  font-size: 1.25rem;
  flex-shrink: 0;
}

.next-interview-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.next-interview-time {
  font-size: 0.9rem;
  font-weight: 600;
  color: $color-warning;
}

.next-interview-round {
  font-size: 0.75rem;
  color: $color-text-secondary;
}

.card-meta {
  display: flex;
  gap: $spacing-lg;
  margin-bottom: $spacing-md;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.meta-label {
  font-size: 0.75rem;
  color: $color-text-tertiary;
}

.meta-value {
  font-size: 0.875rem;
  color: $color-text-secondary;
}

.card-actions {
  display: flex;
  gap: $spacing-sm;
  padding-top: $spacing-md;
  margin-top: auto;
  border-top: 1px solid $color-bg-tertiary;
}

.action-btn {
  flex: 1;
  padding: $spacing-sm $spacing-md;
  border-radius: $radius-md;
  border: none;
  cursor: pointer;
  font-size: 0.875rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-xs;
  transition: all 0.2s;

  background: $color-accent;
  color: $color-bg-primary;

  &:hover {
    background: $color-accent-light;
  }

  &.secondary {
    background: $color-bg-tertiary;
    color: $color-text-secondary;

    &:hover {
      background: color.adjust($color-bg-tertiary, $lightness: 5%);
    }
  }

  &.danger {
    flex: 0 0 auto;
    min-width: 40px;
    padding: $spacing-sm;
    background: transparent;
    color: $color-text-tertiary;
    border: none;

    &:hover {
      color: $color-error;
      background: rgba($color-error, 0.1);
    }

    svg {
      flex-shrink: 0;
    }
  }

  .icon {
    font-size: 1rem;
  }
}
</style>
