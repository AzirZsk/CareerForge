<!--=====================================================
  LandIt 定制简历单个阶段项组件
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
        <span class="stage-label">{{ getTailorStageLabel(item.stage) }}</span>
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
          <!-- JD 分析结果 -->
          <AnalyzeJDStageContent
            v-if="item.stage === 'analyze_jd'"
            :data="item.data"
          />
          <!-- 匹配分析结果 -->
          <MatchResumeStageContent
            v-else-if="item.stage === 'match_resume'"
            :data="item.data"
          />
          <!-- 定制简历结果 -->
          <GenerateTailoredStageContent
            v-else-if="item.stage === 'generate_tailored'"
            :data="item.data"
            :can-show-comparison="canShowComparison"
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
import type { TailorStageHistoryItem } from '@/types/resume-tailor'
import { getTailorStageLabel } from '@/types/resume-tailor'
import AnalyzeJDStageContent from './AnalyzeJDStageContent.vue'
import MatchResumeStageContent from './MatchResumeStageContent.vue'
import GenerateTailoredStageContent from './GenerateTailoredStageContent.vue'

defineProps<{
  item: TailorStageHistoryItem
  isActive: boolean
  elapsed: string
  canShowComparison: boolean
}>()

defineEmits<{
  toggleExpand: []
  showComparison: []
}>()
</script>

<style lang="scss" scoped>
.stage-item {
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
  border: 1px solid rgba(255, 255, 255, 0.05);
  transition: all $transition-fast;

  &.active {
    border-color: rgba(212, 168, 83, 0.3);
  }

  &.completed {
    border-color: rgba(52, 211, 153, 0.2);
  }
}

.stage-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-md;

  &.clickable {
    cursor: pointer;

    &:hover {
      background: rgba(255, 255, 255, 0.03);
    }
  }
}

.stage-left {
  display: flex;
  align-items: center;
  gap: $spacing-md;
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
    background: $color-text-tertiary;
    border-radius: 50%;
  }
}

.stage-label {
  font-size: $text-sm;
  color: $color-text-secondary;

  .completed & {
    color: $color-text-primary;
  }

  .active & {
    color: $color-accent;
  }
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

  &:not(:empty) {
    margin-top: $spacing-sm;
  }
}

// 移除独立滚动，让内容自然撑开，由父容器统一管理滚动
// 解决嵌套滚动导致滚动到底部无法继续滚动父容器的问题
.stage-data {
  padding: $spacing-md;
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
