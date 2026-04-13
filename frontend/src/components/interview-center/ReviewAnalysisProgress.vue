<!--=====================================================
  复盘分析进度组件（内嵌在页面中，可展开查看各阶段详细数据
  @author Azir
=====================================================-->

<template>
  <div class="review-progress">
    <!-- 头部：状态图标 + 标题 -->
    <div class="progress-header">
      <div class="status-icon" :class="statusClass">
        <font-awesome-icon v-if="state.isRunning" icon="fa-solid fa-spinner" class="spinner" />
        <font-awesome-icon v-else-if="state.hasError" icon="fa-solid fa-circle-xmark" />
        <font-awesome-icon v-else icon="fa-solid fa-robot" />
      </div>
      <div class="header-text">
        <h4>{{ title }}</h4>
        <p v-if="state.isRunning" class="header-subtitle">{{ state.message }}</p>
      </div>
      <button
        v-if="!state.isRunning && state.isCompleted"
        class="btn btn-sm btn-reanalyze"
        @click.stop="emit('reanalyze')"
      >
        重新分析
      </button>
    </div>

    <!-- 进度条 -->
    <div class="progress-bar-container">
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: progress + '%' }"></div>
      </div>
      <span class="progress-percent">{{ progress }}%</span>
    </div>

    <!-- 阶段列表 -->
    <div class="stage-list">
      <div
        v-for="item in sortedStageHistory"
        :key="item.stage"
        class="stage-item"
        :class="{
          active: item.stage === state.currentStage && state.isRunning,
          completed: item.completed,
        }"
      >
        <!-- 阶段主内容：可点击展开 -->
        <div
          class="stage-main"
          :class="{ clickable: item.completed && item.data }"
          @click="toggleExpand(item.stage)"
        >
          <div class="stage-left">
            <!-- 阶段指示器 -->
            <div class="stage-indicator">
              <font-awesome-icon v-if="item.completed" icon="fa-solid fa-check" />
              <div v-else-if="item.stage === state.currentStage && state.isRunning" class="spinner" />
              <div v-else class="dot" />
            </div>
            <!-- 阶段标签 -->
            <span class="stage-label">{{ getStageLabel(item.stage) }}</span>
            <!-- 耗时 -->
            <span v-if="item.startTime" class="stage-elapsed" :class="{ running: !item.endTime && item.stage === state.currentStage }">
              {{ formatElapsed(item) }}
            </span>
          </div>
          <!-- 展开指示器 -->
          <div v-if="item.completed && item.data" class="expand-indicator">
            <font-awesome-icon icon="fa-solid fa-chevron-down" :class="{ rotated: item.expanded }" />
          </div>
        </div>

        <!-- 展开的数据区域 -->
        <Transition name="expand">
          <div v-if="item.expanded && item.data" class="stage-data-wrapper">
            <div class="stage-data">
              <!-- 对话分析数据 -->
              <TranscriptAnalysisContent
                v-if="item.stage === 'analyze_transcript'"
                :data="(item.data as TranscriptAnalysisResult)"
              />
              <!-- 面试分析数据 -->
              <InterviewAnalysisContent
                v-else-if="item.stage === 'analyze_interview'"
                :data="(item.data as InterviewAnalysisResult)"
              />
              <!-- 建议列表数据 -->
              <AdviceListContent
                v-else-if="item.stage === 'generate_advice'"
                :data="(item.data as AdviceItem[])"
              />
            </div>
          </div>
        </Transition>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type {
  ReviewAnalysisState,
  ReviewStageHistoryItem,
  ReviewStage,
  TranscriptAnalysisResult,
  InterviewAnalysisResult,
  AdviceItem
} from '@/types/interview-center'
import TranscriptAnalysisContent from './review/TranscriptAnalysisContent.vue'
import InterviewAnalysisContent from './review/InterviewAnalysisContent.vue'
import AdviceListContent from './review/AdviceListContent.vue'
import { useStageTimer } from '@/composables/useStageTimer'

const props = defineProps<{
  state: ReviewAnalysisState
}>()

const emit = defineEmits<{
  (e: 'toggleExpand', stage: ReviewStage): void
  (e: 'reanalyze'): void
}>()

// 计时器 composable（用于计算各阶段耗时)
const { formatElapsed } = useStageTimer(() => props.state.isRunning)

// ==================== 计算属性 ====================

const sortedStageHistory = computed(() => {
  const displayStages: ReviewStage[] = ['analyze_transcript', 'analyze_interview', 'generate_advice']
  return displayStages.map(stage => {
    const historyItem = props.state.stageHistory.find(h => h.stage === stage)
    return historyItem || {
      stage,
      message: '',
      timestamp: 0,
      startTime: undefined,
      endTime: undefined,
      completed: false,
      data: null,
      expanded: false
    } as ReviewStageHistoryItem
  })
})

const statusClass = computed(() => {
  if (props.state.hasError) return 'error'
  if (props.state.isCompleted) return 'success'
  if (props.state.isRunning) return 'running'
  return ''
})

const title = computed(() => {
  if (props.state.hasError) return '分析失败'
  if (props.state.isCompleted) return '分析完成'
  if (props.state.isRunning) return 'AI 复盘分析中...'
  return '复盘分析'
})

const progress = computed(() => props.state.progress)

// ==================== 方法 ====================

function getStageLabel(stage: ReviewStage): string {
  const labels: Record<ReviewStage, string> = {
    analyze_transcript: '分析面试对话',
    analyze_interview: 'AI 分析表现',
    generate_advice: '生成改进建议'
  }
  return labels[stage] || stage
}

function toggleExpand(stage: ReviewStage): void {
  emit('toggleExpand', stage)
}
</script>

<style lang="scss" scoped>
.review-progress {
  padding: $spacing-md;
  background: $color-bg-secondary;
  border-radius: $radius-md;
}

// 头部
.progress-header {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  margin-bottom: $spacing-md;
}

.status-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: $radius-lg;
  background: rgba($color-bg-elevated, 0.2);
  font-size: 1.25rem;

  &.running {
    background: rgba($color-accent, 0.15);
    color: $color-accent;
  }

  &.success {
    background: rgba($color-success, 0.15);
    color: $color-success;
  }

  &.error {
    background: rgba($color-error, 0.15);
    color: $color-error;
  }

  .spinner {
    animation: spin 1s linear infinite;
  }
}

.header-text {
  h4 {
    font-size: 1.125rem;
    font-weight: $weight-semibold;
    color: $color-text-primary;
    margin: 0;
  }

  .header-subtitle {
    font-size: $text-sm;
    color: $color-text-tertiary;
    margin: $spacing-xs 0 0 0;
  }
}

.btn-reanalyze {
  margin-left: auto;
  background: transparent;
  border: 1px solid rgba($color-accent, 0.3);
  color: $color-accent;
  padding: 4px 10px;
  font-size: 0.75rem;
  border-radius: $radius-sm;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: rgba($color-accent, 0.15);
    border-color: $color-accent;
  }
}

// 进度条
.progress-bar-container {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-sm;
}

.progress-bar {
  flex: 1;
  height: 8px;
  background: $color-bg-elevated;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, $color-accent, $color-accent-light);
  transition: width 0.3s ease;
}

.progress-percent {
  font-size: $text-sm;
  font-variant-numeric: tabular-nums;
  min-width: 40px;
  text-align: right;
  font-weight: $weight-medium;
  color: $color-accent;
}

// 阶段列表
.stage-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.stage-item {
  padding: $spacing-md;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
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
  color: $color-text-tertiary;
  transition: color $transition-fast;

  svg {
    transition: transform $transition-fast;

    &.rotated {
      transform: rotate(180deg);
    }
  }
}

// 展开数据区域
.stage-data-wrapper {
  display: grid;
  grid-template-rows: 1fr;
  overflow: hidden;
  max-height: 400px;

  &:not(:empty) {
    margin-top: $spacing-sm;
  }
}

.stage-data {
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: $spacing-sm;
  background: rgba(0, 0, 0, 0.2);
  border-radius: $radius-sm;
}

// 展开动画
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
