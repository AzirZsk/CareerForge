<!--=====================================================
  建议区块容器组件
  位于详情面板头部，可折叠，支持按类型筛选
  @author Azir
=====================================================-->

<template>
  <div v-if="suggestions.length > 0" class="suggestions-block" :class="{ 'is-expanded': isExpanded }">
    <!-- 标题栏（可折叠） -->
    <div class="block-header" @click="toggleExpand">
      <div class="header-left">
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
    <Transition @enter="onEnter" @after-enter="onAfterEnter" @leave="onLeave" @after-leave="onAfterLeave">
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
import { ref } from 'vue'
import SuggestionCard from './SuggestionCard.vue'
import type { ResumeSuggestionItem } from '@/types'

defineProps<{
  suggestions: ResumeSuggestionItem[]
}>()



// 展开状态
const isExpanded = ref<boolean>(false)

// 切换展开状态
function toggleExpand(): void {
  isExpanded.value = !isExpanded.value
}

// 展开/折叠过渡钩子（用真实高度代替 max-height）
function onEnter(el: Element): void {
  const htmlEl = el as HTMLElement
  htmlEl.style.overflow = 'hidden'
  htmlEl.style.height = '0'
  htmlEl.style.opacity = '0'
  void htmlEl.offsetHeight
  htmlEl.style.transition = 'height 0.25s ease, opacity 0.25s ease'
  htmlEl.style.height = htmlEl.scrollHeight + 'px'
  htmlEl.style.opacity = '1'
}

function onAfterEnter(el: Element): void {
  const htmlEl = el as HTMLElement
  htmlEl.style.height = ''
  htmlEl.style.overflow = ''
  htmlEl.style.transition = ''
}

function onLeave(el: Element): void {
  const htmlEl = el as HTMLElement
  htmlEl.style.overflow = 'hidden'
  htmlEl.style.height = htmlEl.scrollHeight + 'px'
  htmlEl.style.opacity = '1'
  void htmlEl.offsetHeight
  htmlEl.style.transition = 'height 0.25s ease, opacity 0.25s ease'
  htmlEl.style.height = '0'
  htmlEl.style.opacity = '0'
}

function onAfterLeave(el: Element): void {
  const htmlEl = el as HTMLElement
  htmlEl.style.height = ''
  htmlEl.style.overflow = ''
  htmlEl.style.transition = ''
  htmlEl.style.opacity = ''
}
</script>

<style lang="scss" scoped>
.suggestions-block {
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
  margin-bottom: $spacing-md;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  overflow: hidden;

  &.is-expanded {
    margin-bottom: $spacing-lg;
    padding-bottom: $spacing-md;
  }
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
  padding: $spacing-md $spacing-lg $spacing-sm;
}

.suggestions-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
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
