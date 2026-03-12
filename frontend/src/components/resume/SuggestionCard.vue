<!--=====================================================
  单条建议卡片组件
  显示建议类型图标、标题、描述和影响说明
  @author Azir
=====================================================-->

<template>
  <div class="suggestion-card" :class="suggestion.type">
    <div class="suggestion-header">
      <span class="suggestion-icon">{{ typeIcon }}</span>
      <span class="suggestion-category">{{ suggestion.category }}</span>
      <span class="suggestion-impact" :class="impactClass">{{ suggestion.impact }}影响</span>
    </div>
    <h4 class="suggestion-title">{{ suggestion.title }}</h4>
    <p class="suggestion-description">{{ suggestion.description }}</p>
    <div class="suggestion-actions">
      <button class="action-btn apply" @click="$emit('apply', suggestion)">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="20 6 9 17 4 12"></polyline>
        </svg>
        应用
      </button>
      <button class="action-btn ignore" @click="$emit('ignore', suggestion)">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="18" y1="6" x2="6" y2="18"></line>
          <line x1="6" y1="6" x2="18" y2="18"></line>
        </svg>
        忽略
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ResumeSuggestionItem, SuggestionType } from '@/types'

const props = defineProps<{
  suggestion: ResumeSuggestionItem
}>()

defineEmits<{
  apply: [suggestion: ResumeSuggestionItem]
  ignore: [suggestion: ResumeSuggestionItem]
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
  padding: $spacing-md;
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

.suggestion-category {
  flex: 1;
  font-size: $text-xs;
  color: $color-text-tertiary;
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

.suggestion-title {
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-text-primary;
  margin-bottom: $spacing-xs;
}

.suggestion-description {
  font-size: $text-xs;
  color: $color-text-secondary;
  line-height: $leading-relaxed;
  margin-bottom: $spacing-md;
}

.suggestion-actions {
  display: flex;
  gap: $spacing-sm;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-xs $spacing-md;
  font-size: $text-xs;
  border-radius: $radius-sm;
  transition: all $transition-fast;

  &.apply {
    background: $color-accent-glow;
    color: $color-accent;

    &:hover {
      background: rgba(212, 168, 83, 0.2);
    }
  }

  &.ignore {
    background: rgba(255, 255, 255, 0.05);
    color: $color-text-tertiary;

    &:hover {
      background: rgba(255, 255, 255, 0.1);
      color: $color-text-secondary;
    }
  }
}
</style>
