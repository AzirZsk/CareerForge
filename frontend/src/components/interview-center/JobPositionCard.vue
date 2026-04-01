<template>
  <div class="job-position-card" @click="$emit('click')">
    <div class="card-header">
      <div class="company-info">
        <h3 class="company-name">{{ jobPosition.companyName }}</h3>
        <span class="position-title">{{ jobPosition.title }}</span>
      </div>
      <div class="interview-badge" v-if="jobPosition.interviewCount > 0">
        {{ jobPosition.interviewCount }} 次面试
      </div>
    </div>

    <div class="card-meta">
      <div class="meta-item" v-if="jobPosition.latestInterviewDate">
        <span class="meta-label">最近面试</span>
        <span class="meta-value">{{ formatDate(jobPosition.latestInterviewDate) }}</span>
      </div>
      <div class="meta-item">
        <span class="meta-label">创建时间</span>
        <span class="meta-value">{{ formatDate(jobPosition.createdAt) }}</span>
      </div>
    </div>

    <div class="card-actions">
      <button class="action-btn" @click.stop="$emit('add-interview', jobPosition)">
        <span class="icon">+</span>
        新建面试
      </button>
      <button class="action-btn secondary" @click.stop="$emit('view-detail', jobPosition)">
        查看详情
      </button>
      <button class="action-btn icon-btn danger" @click.stop="$emit('delete', jobPosition)" title="删除">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="3 6 5 6 21 6" />
          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { JobPositionListItem } from '@/types/job-position'

defineProps<{
  jobPosition: JobPositionListItem
}>()

defineEmits<{
  click: []
  'add-interview': [jobPosition: JobPositionListItem]
  'view-detail': [jobPosition: JobPositionListItem]
  'delete': [jobPosition: JobPositionListItem]
}>()

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}
</script>

<style scoped lang="scss">
.job-position-card {
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
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: $spacing-md;
}

.company-info {
  flex: 1;
}

.company-name {
  font-size: 1.25rem;
  font-weight: 600;
  color: $color-text-primary;
  margin-bottom: $spacing-xs;
}

.position-title {
  font-size: 0.875rem;
  color: $color-text-secondary;
}

.interview-badge {
  background: rgba($color-accent, 0.2);
  color: $color-accent;
  padding: $spacing-xs $spacing-sm;
  border-radius: $radius-sm;
  font-size: 0.75rem;
  font-weight: 500;
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
      background: lighten($color-bg-tertiary, 5%);
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
