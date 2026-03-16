<!--=====================================================
  单条建议卡片组件
  显示建议类型图标、标题、描述和影响说明
  @author Azir
=====================================================-->

<template>
  <div class="suggestion-card" :class="suggestion.type">
    <div class="suggestion-header">
      <span class="suggestion-icon">{{ typeIcon }}</span>
      <span class="suggestion-title">{{ suggestion.title }}</span>
      <span class="suggestion-impact" :class="impactClass">{{ suggestion.impact }}影响</span>
    </div>
    <p class="suggestion-description">{{ suggestion.description }}</p>

  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ResumeSuggestionItem, SuggestionType } from '@/types'

const props = defineProps<{
  suggestion: ResumeSuggestionItem
}>()



// 根据类型返回图标
const typeIcon = computed<string>(() => {
  const icons: Record<SuggestionType, string> = {
    critical: '⚠️',
    improvement: '💡',
    enhancement: '✨'
  }
  return icons[props.suggestion.type] || '💡'
})

// 影响程度样式类
const impactClass = computed<string>(() => {
  const impact = props.suggestion.impact?.toLowerCase()
  if (impact === '高') return 'high'
  if (impact === '中') return 'medium'
  return 'low'
})
</script>

<style lang="scss" scoped>
.suggestion-card {
  padding: $spacing-sm $spacing-md;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
  border-left: 3px solid;
  transition: all $transition-fast;

  &:hover {
    background: rgba(255, 255, 255, 0.04);
  }

  &.critical {
    border-color: $color-error;
  }

  &.improvement {
    border-color: $color-warning;
  }

  &.enhancement {
    border-color: $color-info;
  }
}

.suggestion-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-sm;
}

.suggestion-icon {
  font-size: $text-base;
}

.suggestion-title {
  flex: 1;
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-text-primary;
}

.suggestion-impact {
  font-size: $text-xs;
  padding: 2px $spacing-sm;
  border-radius: $radius-sm;

  &.high {
    background: rgba(248, 113, 113, 0.15);
    color: $color-error;
  }

  &.medium {
    background: rgba(251, 191, 36, 0.15);
    color: $color-warning;
  }

  &.low {
    background: rgba(96, 165, 250, 0.15);
    color: $color-info;
  }
}

.suggestion-description {
  font-size: $text-xs;
  color: $color-text-secondary;
  line-height: $leading-relaxed;
}
</style>
