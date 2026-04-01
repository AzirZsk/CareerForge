<!--=====================================================
  LandIt 面试准备清单进度弹窗
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
          <div class="progress-section">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: state.progress + '%' }"></div>
            </div>
            <div class="progress-info">
              <span class="progress-percent">{{ state.progress }}%</span>
              <span class="progress-message">{{ state.message }}</span>
            </div>
          </div>

          <!-- 阶段列表 -->
          <div class="stage-list">
            <div
              v-for="stage in displayStages"
              :key="stage.id"
              class="stage-item"
              :class="getStageClass(stage.id)"
            >
              <div class="stage-indicator">
                <div class="indicator-dot">
                  <svg v-if="isStageCompleted(stage.id)" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                    <polyline points="20 6 9 17 4 12" />
                  </svg>
                  <div v-else-if="isStageActive(stage.id)" class="active-pulse"></div>
                </div>
                <div v-if="stage.id !== 'generate_preparation'" class="indicator-line"></div>
              </div>
              <div class="stage-content">
                <div class="stage-header">
                  <span class="stage-name">{{ stage.label }}</span>
                  <span v-if="isStageActive(stage.id)" class="stage-status running">进行中</span>
                  <span v-else-if="isStageCompleted(stage.id)" class="stage-status completed">已完成</span>
                </div>
                <p v-if="isStageActive(stage.id) && state.message" class="stage-message">{{ state.message }}</p>
              </div>
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
import { computed, ref, watch } from 'vue'
import { useScrollLock } from '@vueuse/core'
import type { PreparationState, PreparationItem } from '@/types/interview-center'

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

// 阶段定义
const displayStages = [
  { id: 'company_research', label: '公司调研' },
  { id: 'jd_analysis', label: 'JD 分析' },
  { id: 'generate_preparation', label: '生成准备事项' }
]

// 计算属性
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

// 阶段状态判断
function getStageClass(stageId: string): string {
  if (isStageCompleted(stageId)) return 'completed'
  if (isStageActive(stageId)) return 'active'
  return 'pending'
}

function isStageCompleted(stageId: string): boolean {
  const currentIndex = displayStages.findIndex(s => s.id === props.state.currentStage)
  const stageIndex = displayStages.findIndex(s => s.id === stageId)
  // 如果已完成整个流程，所有阶段都完成
  if (props.state.isCompleted) return true
  return stageIndex < currentIndex
}

function isStageActive(stageId: string): boolean {
  return props.state.currentStage === stageId && props.state.isRunning
}

// 事件处理
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

// 监听器
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
  max-width: 600px;
  max-height: 80vh;
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

// 进度条
.progress-section {
  padding: $spacing-lg;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.progress-bar {
  height: 6px;
  background: $color-bg-tertiary;
  border-radius: $radius-full;
  overflow: hidden;
  margin-bottom: $spacing-sm;
}

.progress-fill {
  height: 100%;
  background: $gradient-gold;
  border-radius: $radius-full;
  transition: width 0.3s ease;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-percent {
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-accent;
}

.progress-message {
  font-size: $text-sm;
  color: $color-text-tertiary;
}

// 阶段列表
.stage-list {
  padding: $spacing-lg;
  flex: 1;
  overflow-y: auto;
}

.stage-item {
  display: flex;
  gap: $spacing-md;
  padding: $spacing-sm 0;

  &.completed {
    .stage-indicator .indicator-dot {
      background: $color-success;
      border-color: $color-success;
    }

    .stage-indicator .indicator-line {
      background: $color-success;
    }

    .stage-name {
      color: $color-text-primary;
    }
  }

  &.active {
    .stage-indicator .indicator-dot {
      background: $color-accent;
      border-color: $color-accent;
    }

    .stage-name {
      color: $color-accent;
    }
  }

  &.pending {
    opacity: 0.5;

    .stage-indicator .indicator-dot {
      background: $color-bg-tertiary;
    }
  }
}

.stage-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
}

.indicator-dot {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $color-bg-tertiary;
  color: white;

  svg {
    width: 14px;
    height: 14px;
  }
}

.active-pulse {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: white;
  animation: pulse 1.5s ease-in-out infinite;
}

.indicator-line {
  width: 2px;
  height: 24px;
  background: $color-bg-tertiary;
  margin-top: $spacing-xs;
}

.stage-content {
  flex: 1;
  padding-top: 2px;
}

.stage-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-xs;
}

.stage-name {
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-text-secondary;
}

.stage-status {
  font-size: $text-xs;
  padding: 2px 8px;
  border-radius: $radius-full;

  &.running {
    background: rgba($color-accent, 0.15);
    color: $color-accent;
  }

  &.completed {
    background: rgba($color-success, 0.15);
    color: $color-success;
  }
}

.stage-message {
  font-size: $text-xs;
  color: $color-text-tertiary;
  margin: 0;
}

// 预览区域
.preview-section {
  padding: $spacing-lg;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  max-height: 300px;
  overflow-y: auto;
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
}

// 底部
.modal-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: $spacing-md;
  padding: $spacing-lg;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
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

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}
</style>
