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
        <!-- 建议列表 -->
        <TransitionGroup name="list" tag="div" class="suggestions-list">
          <SuggestionCard
            v-for="suggestion in suggestions"
            :key="suggestion.id"
            :suggestion="suggestion"
          />
        </TransitionGroup>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import SuggestionCard from './SuggestionCard.vue'
import type { ResumeSuggestionItem } from '@/types'

const props = defineProps<{
  suggestions: ResumeSuggestionItem[]
}>()



// 展开状态
const isExpanded = ref<boolean>(false)

// 切换展开状态
function toggleExpand(): void {
  isExpanded.value = !isExpanded.value
}

// 根据最高优先级显示图标
const highestPriorityIcon = computed<string>(() => {
  if (props.suggestions.some(s => s.type === 'critical')) return '⚠️'
  if (props.suggestions.some(s => s.type === 'improvement')) return '💡'
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

.suggestions-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
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
