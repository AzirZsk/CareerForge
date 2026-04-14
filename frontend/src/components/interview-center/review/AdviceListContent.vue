<!--=====================================================
  儿建议列表内容展示组件
  @author Azir
=====================================================-->

<template>
  <div class="advice-list-content">
    <div v-if="data && data.length > 0" class="advice-items">
      <div v-for="(advice, index) in data" :key="index" class="advice-item">
        <div class="advice-header">
          <div class="advice-category" :class="getPriorityClass(advice.priority)">
            {{ advice.category }}
          </div>
          <h4 class="advice-title">{{ advice.title }}</h4>
        </div>
        <p class="advice-description">{{ advice.description }}</p>
        <ul v-if="advice.actionItems && advice.actionItems.length > 0" class="action-list">
          <li v-for="(item, actionIndex) in advice.actionItems" :key="actionIndex" class="action-item">
            <font-awesome-icon icon="fa-solid fa-check" class="check-icon" />
            <span>{{ item }}</span>
          </li>
        </ul>
      </div>
    </div>
    <div v-else class="empty-state">
      <p>暂无改进建议</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { AdviceItem } from '@/types/interview-center'

defineProps<{
  data: AdviceItem[]
}>()

// 获取优先级样式类名
function getPriorityClass(priority: string | undefined): string {
  if (priority === 'high') return 'high'
  if (priority === 'medium') return 'medium'
  return 'low'
}
</script>

<style lang="scss" scoped>
.advice-list-content {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.advice-items {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.advice-item {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
  padding: $spacing-sm;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
  transition: all $transition-fast;

  &:hover {
    background: rgba(255, 255, 255, 0.04);
  }
}

.advice-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.advice-category {
  font-size: 0.625rem;
  padding: 2px 8px;
  border-radius: $radius-sm;
  font-weight: $weight-medium;

  &.high {
    background: rgba($color-error, 0.15);
    color: $color-error;
  }
  &.medium {
    background: rgba($color-warning, 0.15);
    color: $color-warning;
  }
  &.low {
    background: rgba($color-info, 0.15);
    color: $color-info;
  }
}

.advice-title {
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-text-primary;
}

.advice-description {
  font-size: $text-sm;
  color: $color-text-secondary;
  line-height: 1.6;
}

.action-list {
  list-style: none;
  padding-left: $spacing-md;

  li {
    display: flex;
    align-items: center;
    gap: $spacing-xs;
    font-size: $text-sm;
    color: $color-text-secondary;
    line-height: 1.5;

    .check-icon {
      color: $color-success;
      font-size: 0.875rem;
      flex-shrink: 0;
    }
  }
}

.empty-state {
  text-align: center;
  padding: $spacing-xl;
  color: $color-text-tertiary;
}
</style>
