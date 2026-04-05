<script setup lang="ts">
import { computed } from 'vue'
import type { PreparationVO, PreparationPriority } from '@/types/interview-center'
import { PRIORITY_CONFIG } from '@/types/interview-center'

const props = defineProps<{
  preparations: PreparationVO[]
}>()

// 按优先级分组统计
const priorityStats = computed(() => {
  const stats: Record<PreparationPriority, { total: number; completed: number }> = {
    required: { total: 0, completed: 0 },
    recommended: { total: 0, completed: 0 },
    optional: { total: 0, completed: 0 }
  }

  for (const item of props.preparations) {
    const priority = item.priority || 'recommended'
    if (stats[priority]) {
      stats[priority].total++
      if (item.completed) {
        stats[priority].completed++
      }
    }
  }

  return stats
})

// 总体进度
const overallProgress = computed(() => {
  const total = props.preparations.length
  if (total === 0) return 0
  const completed = props.preparations.filter((p) => p.completed).length
  return Math.round((completed / total) * 100)
})

// 进度条样式
const progressStyle = computed(() => ({
  width: `${overallProgress.value}%`
}))

// 优先级样式类映射
function getPriorityClass(priority: string): string {
  const classMap: Record<string, string> = {
    required: 'high',
    recommended: 'medium',
    optional: 'low'
  }
  return classMap[priority] || 'medium'
}
</script>

<template>
  <div class="preparation-progress">
    <div class="progress-header">
      <span class="progress-label">准备进度</span>
      <span class="progress-value">{{ overallProgress }}%</span>
    </div>
    <div class="progress-bar-bg">
      <div class="progress-bar-fill" :style="progressStyle"></div>
    </div>
    <div class="priority-stats">
      <template v-for="(config, priority) in PRIORITY_CONFIG" :key="priority">
        <div
          v-if="priorityStats[priority].total > 0"
          class="priority-stat-item"
          :class="getPriorityClass(priority)"
        >
          <span class="priority-dot" />
          <span class="priority-label">{{ config.label }}</span>
          <span class="priority-count">
            {{ priorityStats[priority].completed }}/{{ priorityStats[priority].total }}
          </span>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped lang="scss">
.preparation-progress {
  background: $color-bg-secondary;
  border-radius: $radius-md;
  padding: $spacing-md $spacing-lg;
  margin-bottom: $spacing-lg;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-sm;
}

.progress-label {
  color: $color-text-secondary;
  font-size: 0.875rem;
}

.progress-value {
  color: $color-accent;
  font-weight: 600;
  font-size: 1rem;
}

.progress-bar-bg {
  height: 8px;
  background: $color-bg-tertiary;
  border-radius: $radius-full;
  overflow: hidden;
  margin-bottom: $spacing-md;
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, $color-accent, $color-accent-light);
  border-radius: $radius-full;
  transition: width 0.3s ease;
}

.priority-stats {
  display: flex;
  gap: $spacing-lg;
  flex-wrap: wrap;
}

.priority-stat-item {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: 0.875rem;
  padding: 4px 12px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: $radius-full;

  &.high {
    .priority-dot {
      background: $color-error;
    }
    .priority-count {
      color: $color-error;
    }
  }

  &.medium {
    .priority-dot {
      background: $color-warning;
    }
    .priority-count {
      color: $color-warning;
    }
  }

  &.low {
    .priority-dot {
      background: $color-success;
    }
    .priority-count {
      color: $color-success;
    }
  }
}

.priority-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.priority-label {
  color: $color-text-secondary;
}

.priority-count {
  font-weight: 600;
  min-width: 32px;
  text-align: right;
}
</style>
