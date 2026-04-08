<!--=====================================================
  LandIt 单个阶段项组件
  @author Azir
=====================================================-->

<template>
  <div
    class="stage-item"
    :class="{
      active: isActive,
      completed: item.completed
    }"
  >
    <div
      class="stage-main"
      :class="{ clickable: item.completed && item.data }"
      @click="item.completed && item.data && $emit('toggleExpand')"
    >
      <div class="stage-left">
        <!-- 阶段指示器 -->
        <div class="stage-indicator">
          <font-awesome-icon
            v-if="item.completed"
            icon="fa-solid fa-check"
          />
          <div
            v-else-if="isActive"
            class="spinner"
          />
          <div
            v-else
            class="dot"
          />
        </div>
        <!-- 阶段标签 -->
        <span class="stage-label">{{ getStageLabel(item.stage) }}</span>
        <!-- 耗时 -->
        <span
          v-if="item.startTime"
          class="stage-elapsed"
          :class="{ running: !item.endTime && isActive }"
        >
          {{ elapsed }}
        </span>
      </div>
      <!-- 展开指示器 -->
      <div
        v-if="item.completed && item.data"
        class="expand-indicator"
      >
        <font-awesome-icon
          icon="fa-solid fa-chevron-down"
          :class="{ rotated: item.expanded }"
        />
      </div>
    </div>

    <!-- 展开的数据区域 -->
    <Transition name="expand">
      <div
        v-if="item.expanded && item.data"
        class="stage-data-wrapper"
      >
        <div class="stage-data">
          <!-- 诊断数据 -->
          <DiagnoseStageContent
            v-if="item.stage === 'diagnose_quick' || item.stage === 'diagnose_precise'"
            :data="item.data"
            :stage="item.stage"
          />
          <!-- 生成建议数据 -->
          <SuggestionsStageContent
            v-else-if="item.stage === 'generate_suggestions'"
            :data="item.data"
          />
          <!-- 内容优化数据 -->
          <OptimizeStageContent
            v-else-if="item.stage === 'optimize_section'"
            :data="item.data"
            @show-comparison="$emit('showComparison')"
          />
          <!-- 默认 JSON 显示 -->
          <div
            v-else
            class="data-content"
          >
            <pre class="json-display">{{ JSON.stringify(item.data, null, 2) }}</pre>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import type { StageHistoryItem } from '@/types/resume-optimize'
import { getStageLabel } from '@/types/resume-optimize'
import DiagnoseStageContent from './DiagnoseStageContent.vue'
import SuggestionsStageContent from './SuggestionsStageContent.vue'
import OptimizeStageContent from './OptimizeStageContent.vue'

defineProps<{
  item: StageHistoryItem
  isActive: boolean
  elapsed: string
}>()

defineEmits<{
  toggleExpand: []
  showComparison: []
}>()
</script>

<style lang="scss" scoped>
.stage-item {
  padding: $spacing-md;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
  margin-bottom: $spacing-sm;
  border: 1px solid transparent;
  transition: all $transition-fast;

  &.active {
    border-color: rgba(212, 168, 83, 0.3);
    background: rgba(212, 168, 83, 0.05);
  }

  &.completed {
    border-color: rgba(52, 211, 153, 0.2);
  }
}

.stage-main {
  display: flex;
  justify-content: space-between;
  align-items: center;

  &.clickable {
    cursor: pointer;

    &:hover {
      .stage-label {
        color: $color-text-primary;
      }
      .expand-indicator {
        color: $color-accent;
      }
    }
  }
}

.stage-left {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.stage-indicator {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: $color-success;

  .spinner {
    width: 16px;
    height: 16px;
    border: 2px solid rgba(212, 168, 83, 0.3);
    border-top-color: $color-accent;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }

  .dot {
    width: 8px;
    height: 8px;
    background: rgba(255, 255, 255, 0.2);
    border-radius: 50%;
  }
}

.stage-label {
  font-size: $text-sm;
  color: $color-text-secondary;
}

.stage-elapsed {
  font-size: $text-xs;
  color: $color-text-tertiary;
  font-variant-numeric: tabular-nums;
  min-width: 40px;

  &.running {
    color: $color-accent;
  }
}

.expand-indicator {
  display: flex;
  align-items: center;
  font-size: 16px;
  color: $color-text-tertiary;
  transition: color $transition-fast;

  .rotated {
    transform: rotate(180deg);
    transition: transform $transition-fast;
  }
}

// 展开数据区域 - 使用 Grid 动画实现平滑展开
.stage-data-wrapper {
  display: grid;
  grid-template-rows: 1fr;
  overflow: hidden;
  max-height: 400px; // 限制展开区域最大高度，防止撑满整个滚动容器

  &:not(:empty) {
    margin-top: $spacing-sm;
  }
}

.stage-data {
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  // 移除 overscroll-behavior: contain，允许滚动链传递
  // 当内部滚动到边界时，自动触发外层滚动
}

.data-content {
  font-size: $text-sm;
}

// JSON 显示
.json-display {
  font-size: $text-xs;
  color: $color-text-tertiary;
  background: rgba(0, 0, 0, 0.2);
  padding: $spacing-sm;
  border-radius: $radius-sm;
  overflow-x: auto;
  max-height: 200px;
}

// 展开动画 - 使用 Grid 实现平滑展开，告别卡顿
.expand-enter-active {
  transition: grid-template-rows 0.25s ease-out, opacity 0.2s ease-out;
}

.expand-leave-active {
  transition: grid-template-rows 0.2s ease-in, opacity 0.15s ease-in;
}

.expand-enter-from,
.expand-leave-to {
  grid-template-rows: 0fr;
  opacity: 0;
}

.expand-enter-to,
.expand-leave-from {
  grid-template-rows: 1fr;
  opacity: 1;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
