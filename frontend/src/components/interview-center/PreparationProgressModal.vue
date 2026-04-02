<!--=====================================================
  LandIt 面试准备清单进度弹窗（全屏模式）
  @author Azir
=====================================================-->

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="visible"
        class="modal-overlay"
      >
        <div class="modal-container">
          <!-- 头部 -->
          <div class="modal-header">
            <div class="header-left">
              <div class="header-icon" :class="headerClass">
                <svg v-if="state.isRunning" class="spinner" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10" stroke-opacity="0.3" />
                  <path d="M12 2a10 10 0 0 1 10 10" />
                </svg>
                <svg v-else-if="state.hasError" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="15" y1="9" x2="9" y2="15" />
                  <line x1="9" y1="9" x2="15" y2="15" />
                </svg>
                <svg v-else-if="state.isCompleted" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                  <polyline points="22 4 12 14.01 9 11.01" />
                </svg>
                <svg v-else width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
                </svg>
              </div>
              <div class="header-title">
                <h3>{{ headerTitle }}</h3>
                <p v-if="state.isRunning" class="header-subtitle">{{ state.message }}</p>
              </div>
            </div>
            <button class="close-btn" @click="handleClose">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          </div>

          <!-- 进度条 -->
          <ProgressBar
            :progress="state.progress"
            :message="state.message"
          />

          <!-- 阶段列表 -->
          <div class="stage-list">
            <div
              v-for="item in sortedStageHistory"
              :key="item.stage"
              class="stage-item"
              :class="{
                active: item.stage === state.currentStage && state.isRunning,
                completed: item.completed
              }"
            >
              <div
                class="stage-main"
                :class="{ clickable: item.completed && item.data }"
                @click="item.completed && item.data && toggleExpand(item.stage)"
              >
                <div class="stage-left">
                  <!-- 阶段指示器 -->
                  <div class="stage-indicator">
                    <svg
                      v-if="item.completed"
                      width="16"
                      height="16"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                    >
                      <polyline points="20 6 9 17 4 12" />
                    </svg>
                    <div
                      v-else-if="item.stage === state.currentStage && state.isRunning"
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
                    :class="{ running: !item.endTime && item.stage === state.currentStage && state.isRunning }"
                  >
                    {{ formatElapsed(item) }}
                  </span>
                </div>
                <!-- 展开指示器 -->
                <div
                  v-if="item.completed && item.data"
                  class="expand-indicator"
                >
                  <svg
                    width="16"
                    height="16"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                    :class="{ rotated: item.expanded }"
                  >
                    <polyline points="6 9 12 15 18 9" />
                  </svg>
                </div>
              </div>

              <!-- 展开的数据区域 -->
              <Transition name="expand">
                <div
                  v-if="item.expanded && item.data"
                  class="stage-data-wrapper"
                >
                  <div class="stage-data">
                    <!-- 公司调研数据 -->
                    <div v-if="item.stage === 'company_research'" class="data-section">
                      <CompanyResearchContent :data="(item.data as CompanyResearchResult)" />
                    </div>
                    <!-- JD 分析数据 -->
                    <div v-else-if="item.stage === 'jd_analysis'" class="data-section">
                      <JDAnalysisContent :data="(item.data as JDAnalysisResult)" />
                    </div>
                    <!-- 准备事项数据 -->
                    <div v-else-if="item.stage === 'generate_preparation'" class="data-section">
                      <PreparationItemsContent :items="(item.data as PreparationItem[])" />
                    </div>
                    <!-- 默认显示 -->
                    <div v-else class="data-content">
                      <pre class="json-display">{{ JSON.stringify(item.data, null, 2) }}</pre>
                    </div>
                  </div>
                </div>
              </Transition>
            </div>
          </div>

          <!-- 完成后预览 -->
          <div v-if="state.isCompleted && state.preparationItems.length > 0" class="preview-section">
            <div class="preview-header">
              <h4>AI 生成的准备事项</h4>
              <span class="preview-count">{{ state.preparationItems.length }} 项</span>
            </div>
            <div class="preview-list">
              <div
                v-for="(item, index) in state.preparationItems"
                :key="index"
                class="preview-item"
              >
                <input
                  type="checkbox"
                  v-model="selectedItems[index]"
                  class="item-checkbox"
                />
                <div class="item-content">
                  <span class="item-title">{{ item.title }}</span>
                  <span v-if="item.description" class="item-desc">{{ item.description }}</span>
                  <div v-if="item.category || item.priority" class="item-meta">
                    <span v-if="item.category" class="meta-tag category">{{ item.category }}</span>
                    <span v-if="item.priority" class="meta-tag priority" :class="item.priority">
                      {{ priorityLabels[item.priority] }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 错误提示 -->
          <div v-if="state.hasError" class="error-section">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            <span>{{ state.errorMessage || '生成失败，请重试' }}</span>
          </div>

          <!-- 底部操作 -->
          <div class="modal-footer">
            <template v-if="state.hasError">
              <button class="btn btn-secondary" @click="handleClose">关闭</button>
              <button class="btn btn-primary" @click="handleRetry">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="23 4 23 10 17 10" />
                  <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
                </svg>
                重试
              </button>
            </template>
            <template v-else-if="state.isCompleted && state.preparationItems.length > 0">
              <button class="btn btn-secondary" @click="handleClose">取消</button>
              <button
                class="btn btn-primary"
                :disabled="selectedCount === 0"
                @click="handleSave"
              >
                保存选中 ({{ selectedCount }})
              </button>
            </template>
            <template v-else-if="state.isCompleted">
              <button class="btn btn-secondary" @click="handleClose">关闭</button>
            </template>
            <template v-else>
              <button class="btn btn-secondary" @click="handleCancel">取消</button>
            </template>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, ref, watch, onUnmounted } from 'vue'
import { useScrollLock } from '@vueuse/core'
import type {
  PreparationState,
  PreparationItem,
  PreparationStage,
  PreparationStageHistoryItem,
  CompanyResearchResult,
  JDAnalysisResult
} from '@/types/interview-center'
import { useInterviewPreparation } from '@/composables/useInterviewPreparation'
import ProgressBar from '@/components/resume/optimize/ProgressBar.vue'
import CompanyResearchContent from './preparation/CompanyResearchContent.vue'
import JDAnalysisContent from './preparation/JDAnalysisContent.vue'
import PreparationItemsContent from './preparation/PreparationItemsContent.vue'

const props = defineProps<{
  visible: boolean
  state: PreparationState
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'save': [items: PreparationItem[]]
  'cancel': []
  'retry': []
}>()

// 锁定背景滚动
const isScrollLocked = useScrollLock(document.body)

// 勾选状态
const selectedItems = ref<Record<number, boolean>>({})

// 优先级标签
const priorityLabels: Record<string, string> = {
  high: '高',
  medium: '中',
  low: '低'
}

// 获取 composable 方法
const { toggleExpand, getStageLabel } = useInterviewPreparation()

// ==================== 计时器 ====================
const now = ref(Date.now())
let timerHandle: ReturnType<typeof setInterval> | null = null

// 启动计时器
function startTimer() {
  if (!timerHandle) {
    timerHandle = setInterval(() => {
      now.value = Date.now()
    }, 1000)
  }
}

// 停止计时器
function stopTimer() {
  if (timerHandle) {
    clearInterval(timerHandle)
    timerHandle = null
  }
}

// 格式化耗时
function formatElapsed(item: PreparationStageHistoryItem): string {
  if (!item.startTime) return ''
  const end = item.endTime ?? now.value
  const elapsed = Math.max(0, Math.floor((end - item.startTime) / 1000))
  const m = String(Math.floor(elapsed / 60)).padStart(2, '0')
  const s = String(elapsed % 60).padStart(2, '0')
  return `${m}:${s}`
}

// 监听运行状态，自动启动/停止计时器
watch(
  () => props.state.isRunning,
  (running) => {
    if (running) {
      startTimer()
    } else {
      stopTimer()
    }
  },
  { immediate: true }
)

// 组件卸载时清理
onUnmounted(() => {
  stopTimer()
})

// ==================== 计算属性 ====================

// 排序后的阶段历史
const sortedStageHistory = computed(() => {
  const displayStages: PreparationStage[] = ['company_research', 'jd_analysis', 'generate_preparation']
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
    } as PreparationStageHistoryItem
  })
})

const headerClass = computed(() => {
  if (props.state.hasError) return 'error'
  if (props.state.isCompleted) return 'success'
  if (props.state.isRunning) return 'running'
  return 'idle'
})

const headerTitle = computed(() => {
  if (props.state.hasError) return '生成失败'
  if (props.state.isCompleted) return '生成完成'
  if (props.state.isRunning) return 'AI 生成准备清单...'
  return 'AI 生成准备清单'
})

const selectedCount = computed(() => {
  return Object.values(selectedItems.value).filter(Boolean).length
})

// ==================== 事件处理 ====================

function handleClose() {
  emit('update:visible', false)
}

function handleCancel() {
  emit('cancel')
  emit('update:visible', false)
}

function handleSave() {
  const itemsToSave = props.state.preparationItems.filter((_, index) => selectedItems.value[index])
  emit('save', itemsToSave)
}

function handleRetry() {
  emit('retry')
}

// ==================== 监听器 ====================

watch(
  () => props.visible,
  (visible) => {
    isScrollLocked.value = visible
    if (visible) {
      // 重置勾选状态
      selectedItems.value = {}
      // 默认全选
      props.state.preparationItems.forEach((_, index) => {
        selectedItems.value[index] = true
      })
    }
  },
  { immediate: true }
)

// 监听准备事项变化，自动全选新项
watch(
  () => props.state.preparationItems,
  (items) => {
    items.forEach((_, index) => {
      if (selectedItems.value[index] === undefined) {
        selectedItems.value[index] = true
      }
    })
  }
)
</script>

<style lang="scss" scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: $z-modal;
  padding: $spacing-xl;
}

.modal-container {
  width: 100%;
  height: 100%;
  max-width: 100%;
  max-height: 100%;
  background: $color-bg-primary;
  border-radius: $radius-xl;
  border: 1px solid rgba(255, 255, 255, 0.08);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

// 头部
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-lg;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.header-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: $radius-lg;
  background: rgba(255, 255, 255, 0.05);

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

.header-title {
  h3 {
    font-size: 1.125rem;
    font-weight: $weight-semibold;
    color: $color-text-primary;
    margin: 0;
  }

  .header-subtitle {
    font-size: $text-sm;
    color: $color-text-tertiary;
    margin: $spacing-xs 0 0;
  }
}

.close-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $color-text-tertiary;
  border-radius: $radius-md;
  transition: all $transition-fast;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: $color-text-primary;
  }
}

// 阶段列表
.stage-list {
  padding: $spacing-lg;
  flex: 1;
  overflow-y: auto;
}

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
}

.data-section {
  padding: $spacing-sm;
  background: rgba(0, 0, 0, 0.2);
  border-radius: $radius-sm;
}

.data-content {
  font-size: $text-sm;
}

.json-display {
  font-size: $text-xs;
  color: $color-text-tertiary;
  background: rgba(0, 0, 0, 0.2);
  padding: $spacing-sm;
  border-radius: $radius-sm;
  overflow-x: auto;
  max-height: 200px;
  margin: 0;
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

// 预览区域
.preview-section {
  padding: $spacing-lg;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  max-height: 300px;
  overflow-y: auto;
  flex-shrink: 0;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $spacing-md;

  h4 {
    font-size: $text-sm;
    font-weight: $weight-semibold;
    color: $color-text-primary;
    margin: 0;
  }

  .preview-count {
    font-size: $text-xs;
    color: $color-text-tertiary;
  }
}

.preview-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.preview-item {
  display: flex;
  gap: $spacing-sm;
  padding: $spacing-sm;
  background: $color-bg-secondary;
  border-radius: $radius-md;
  transition: background $transition-fast;

  &:hover {
    background: $color-bg-tertiary;
  }
}

.item-checkbox {
  width: 18px;
  height: 18px;
  accent-color: $color-accent;
  flex-shrink: 0;
  margin-top: 2px;
}

.item-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.item-title {
  font-size: $text-sm;
  color: $color-text-primary;
}

.item-desc {
  font-size: $text-xs;
  color: $color-text-tertiary;
  line-height: 1.5;
}

.item-meta {
  display: flex;
  gap: $spacing-xs;
  margin-top: 4px;
}

.meta-tag {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: $radius-sm;

  &.category {
    background: $color-bg-tertiary;
    color: $color-text-tertiary;
  }

  &.priority {
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
}

// 错误提示
.error-section {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-md $spacing-lg;
  background: rgba($color-error, 0.1);
  color: $color-error;
  font-size: $text-sm;
  flex-shrink: 0;
}

// 底部
.modal-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: $spacing-md;
  padding: $spacing-lg;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  flex-shrink: 0;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-lg;
  font-size: $text-sm;
  font-weight: $weight-medium;
  border-radius: $radius-md;
  transition: all $transition-fast;
  cursor: pointer;

  &.btn-primary {
    background: $gradient-gold;
    color: $color-bg-deep;
    border: none;

    &:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(212, 168, 83, 0.3);
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }

  &.btn-secondary {
    background: transparent;
    color: $color-text-secondary;
    border: 1px solid rgba(255, 255, 255, 0.1);

    &:hover {
      background: rgba(255, 255, 255, 0.05);
      color: $color-text-primary;
    }
  }
}

// 动画
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s ease;

  .modal-container {
    transition: transform 0.3s ease;
  }
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;

  .modal-container {
    transform: scale(0.95);
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
