<!--=====================================================
  建议区块容器组件
  位于详情面板头部，可折叠，支持按类型筛选
  @author Azir
=====================================================-->

<template>
  <div v-if="suggestions.length > 0" class="suggestions-block">
    <!-- 标题栏（可折叠） -->
    <div class="block-header" @click="toggleExpand">
      <div class="header-left">
        <span class="block-icon">{{ highestPriorityIcon }}</span>
        <span class="block-title">优化建议</span>
        <span class="block-count">({{ suggestions.length }})</span>
      </div>
      <div class="header-right">
        <svg
          class="expand-icon"
          :class="{ expanded: isExpanded }"
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <polyline points="6 9 12 15 18 9"></polyline>
        </svg>
      </div>
    </div>

    <!-- 展开内容 -->
    <Transition name="expand">
      <div v-show="isExpanded" class="block-content">
        <!-- 类型筛选按钮 -->
        <div class="filter-tabs">
          <button
            class="filter-tab"
            :class="{ active: activeFilter === 'all' }"
            @click="activeFilter = 'all'"
          >
            全部
          </button>
          <button
            v-if="criticalCount > 0"
            class="filter-tab critical"
            :class="{ active: activeFilter === 'critical' }"
            @click="activeFilter = 'critical'"
          >
            ⚠️ {{ criticalCount }}
          </button>
          <button
            v-if="improvementCount > 0"
            class="filter-tab improvement"
            :class="{ active: activeFilter === 'improvement' }"
            @click="activeFilter = 'improvement'"
          >
            💡 {{ improvementCount }}
          </button>
          <button
            v-if="enhancementCount > 0"
            class="filter-tab enhancement"
            :class="{ active: activeFilter === 'enhancement' }"
            @click="activeFilter = 'enhancement'"
          >
            ✨ {{ enhancementCount }}
          </button>
        </div>

        <!-- 建议列表 -->
        <TransitionGroup name="list" tag="div" class="suggestions-list">
          <SuggestionCard
            v-for="suggestion in filteredSuggestions"
            :key="suggestion.id"
            :suggestion="suggestion"
            @apply="$emit('apply', $event)"
            @ignore="$emit('ignore', $event)"
          />
        </TransitionGroup>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import SuggestionCard from './SuggestionCard.vue'
import type { ResumeSuggestionItem, SuggestionType } from '@/types'

const props = defineProps<{
  suggestions: ResumeSuggestionItem[]
}>()

defineEmits<{
  apply: [suggestion: ResumeSuggestionItem]
  ignore: [suggestion: ResumeSuggestionItem]
}>()

// 展开状态
const isExpanded = ref<boolean>(false)

// 当前筛选类型
const activeFilter = ref<'all' | SuggestionType>('all')

// 切换展开状态
function toggleExpand(): void {
  isExpanded.value = !isExpanded.value
}

// 按类型统计数量
const criticalCount = computed<number>(() => {
  return props.suggestions.filter(s => s.type === 'critical').length
})

const improvementCount = computed<number>(() => {
  return props.suggestions.filter(s => s.type === 'improvement').length
})

const enhancementCount = computed<number>(() => {
  return props.suggestions.filter(s => s.type === 'enhancement').length
})

// 筛选后的建议列表
const filteredSuggestions = computed<ResumeSuggestionItem[]>(() => {
  if (activeFilter.value === 'all') {
    return props.suggestions
  }
  return props.suggestions.filter(s => s.type === activeFilter.value)
})

// 根据最高优先级显示图标
const highestPriorityIcon = computed<string>(() => {
  if (criticalCount.value > 0) return '⚠️'
  if (improvementCount.value > 0) return '💡'
  return '✨'
})
</script>

<style lang="scss" scoped>
.suggestions-block {
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
  margin-bottom: $spacing-lg;
  overflow: hidden;
}

.block-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-md $spacing-lg;
  cursor: pointer;
  transition: background $transition-fast;

  &:hover {
    background: rgba(255, 255, 255, 0.02);
  }
}

.header-left {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.block-icon {
  font-size: $text-lg;
}

.block-title {
  font-size: $text-base;
  font-weight: $weight-medium;
  color: $color-text-primary;
}

.block-count {
  font-size: $text-sm;
  color: $color-text-tertiary;
}

.expand-icon {
  transition: transform $transition-fast;

  &.expanded {
    transform: rotate(180deg);
  }
}

.block-content {
  padding: 0 $spacing-lg $spacing-lg;
}

.filter-tabs {
  display: flex;
  gap: $spacing-sm;
  margin-bottom: $spacing-md;
  flex-wrap: wrap;
}

.filter-tab {
  padding: $spacing-xs $spacing-md;
  font-size: $text-xs;
  background: rgba(255, 255, 255, 0.05);
  color: $color-text-tertiary;
  border-radius: $radius-sm;
  transition: all $transition-fast;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: $color-text-secondary;
  }

  &.active {
    background: $color-accent-glow;
    color: $color-accent;
  }

  &.critical.active {
    background: rgba(248, 113, 113, 0.15);
    color: $color-error;
  }

  &.improvement.active {
    background: rgba(251, 191, 36, 0.15);
    color: $color-warning;
  }

  &.enhancement.active {
    background: rgba(96, 165, 250, 0.15);
    color: $color-info;
  }
}

.suggestions-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

// 展开动画
.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  max-height: 0;
}

.expand-enter-to,
.expand-leave-from {
  opacity: 1;
  max-height: 1000px;
}

// 列表动画
.list-enter-active,
.list-leave-active {
  transition: all 0.3s ease;
}

.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateX(-10px);
}

.list-move {
  transition: transform 0.3s ease;
}
</style>
