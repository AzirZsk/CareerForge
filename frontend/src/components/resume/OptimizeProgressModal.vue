<!--=====================================================
  LandIt 简历优化进度弹窗
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
          <ModalHeader
            :is-optimizing="state.isOptimizing"
            :is-completed="state.isCompleted"
            :has-error="state.hasError"
            @close="handleClose"
          />

          <!-- 目标岗位 -->
          <div
            v-if="state.targetPosition"
            class="target-info"
          >
            <span class="target-label">目标岗位</span>
            <span class="target-value">{{ state.targetPosition }}</span>
          </div>

          <!-- 进度条 -->
          <ProgressBar
            :progress="state.progress"
            :message="state.message"
          />

          <!-- 阶段列表 -->
          <StageList
            :stage-history="state.stageHistory"
            :current-stage="state.currentStage"
            :is-optimizing="state.isOptimizing"
            :format-elapsed="formatElapsed"
            @toggle-expand="handleToggleExpand"
            @show-comparison="showFullComparison = true"
          />

          <!-- 底部操作 -->
          <div class="modal-footer">
            <template v-if="state.hasError">
              <button
                class="footer-btn secondary"
                @click="handleClose"
              >
                退出
              </button>
              <button
                class="footer-btn primary"
                @click="handleRetry"
              >
                <svg
                  width="16"
                  height="16"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <polyline points="23 4 23 10 17 10" />
                  <polyline points="1 20 1 14 7 14" />
                  <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
                </svg>
                重试
              </button>
            </template>
            <template v-else-if="state.isOptimizing">
              <button
                class="footer-btn secondary"
                @click="handleCancel"
              >
                退出
              </button>
            </template>
            <template v-else-if="state.isCompleted">
              <div
                v-if="showCloseConfirm"
                class="close-confirm-group"
              >
                <span class="close-confirm-text">未应用的变更将丢失</span>
                <button
                  class="footer-btn danger-text"
                  @click="confirmClose"
                >
                  确认退出
                </button>
                <button
                  class="footer-btn secondary"
                  @click="showCloseConfirm = false"
                >
                  取消
                </button>
              </div>
              <template v-else>
                <!-- 应用变更提示 -->
                <div
                  v-if="hasOptimizedSections && !state.isApplying"
                  class="apply-hint"
                >
                  <svg
                    width="14"
                    height="14"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <circle
                      cx="12"
                      cy="12"
                      r="10"
                    />
                    <line
                      x1="12"
                      y1="16"
                      x2="12"
                      y2="12"
                    />
                    <line
                      x1="12"
                      y1="8"
                      x2="12.01"
                      y2="8"
                    />
                  </svg>
                  <span>应用后将重新进行 AI 评分</span>
                </div>
                <button
                  v-if="hasEdits"
                  class="footer-btn secondary"
                  @click="resetEdits"
                >
                  <svg
                    width="16"
                    height="16"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <polyline points="1 4 1 10 7 10" />
                    <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10" />
                  </svg>
                  重置
                </button>
                <button
                  class="footer-btn secondary"
                  @click="handleCloseClick"
                >
                  退出
                </button>
                <button
                  class="footer-btn primary"
                  :class="{ loading: state.isApplying }"
                  :disabled="state.isApplying || !hasOptimizedSections"
                  @click="handleApply"
                >
                  <svg
                    v-if="state.isApplying"
                    class="spinner-icon"
                    width="16"
                    height="16"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <circle
                      cx="12"
                      cy="12"
                      r="10"
                      stroke-opacity="0.3"
                    />
                    <path d="M12 2a10 10 0 0 1 10 10" />
                  </svg>
                  {{ state.isApplying ? '评分中...' : '应用变更' }}
                </button>
              </template>
            </template>
            <template v-else>
              <button
                class="footer-btn secondary"
                @click="handleClose"
              >
                关闭
              </button>
            </template>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 全屏简历对比 -->
    <Teleport to="body">
      <Transition name="fullscreen-comparison">
        <div
          v-if="showFullComparison"
          class="fullscreen-comparison-overlay"
        >
          <div class="fullscreen-comparison-container">
            <div class="fullscreen-comparison-header">
              <h3 class="fullscreen-comparison-title">
                简历对比
              </h3>
              <button
                class="fullscreen-comparison-close"
                @click="showFullComparison = false"
              >
                <svg
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <line
                    x1="18"
                    y1="6"
                    x2="6"
                    y2="18"
                  />
                  <line
                    x1="6"
                    y1="6"
                    x2="18"
                    y2="18"
                  />
                </svg>
              </button>
            </div>
            <div class="fullscreen-comparison-body">
              <ResumeComparison
                v-if="fullComparisonData"
                :before-section="fullComparisonData.beforeSection"
                :after-section="fullComparisonData.afterSection"
                :changes="fullComparisonData.changes"
                :improvement-score="fullComparisonData.improvementScore"
                :before-resume="fullComparisonData.beforeResume"
                :editable="state.isCompleted"
                @edit-section="handleEditSection"
              />
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 编辑区块弹窗 -->
    <EditSectionModal
      v-model:visible="showEditModal"
      :section="editingSection"
      :item-index="editingItemIndex ?? undefined"
      :overlay="showFullComparison"
      @save="handleEditSave"
      @cancel="handleEditCancel"
    />
  </Teleport>
</template>

<script setup lang="ts">
import { computed, ref, watch, toRef } from 'vue'
import { useScrollLock } from '@vueuse/core'
import type { OptimizeState, OptimizeStage, ResumeSection } from '@/types/resume-optimize'
import { useStageTimer } from '@/composables/useStageTimer'
import { useStageEdit } from '@/composables/useStageEdit'
import ModalHeader from './optimize/ModalHeader.vue'
import ProgressBar from './optimize/ProgressBar.vue'
import StageList from './optimize/StageList.vue'
import ResumeComparison from './ResumeComparison.vue'
import EditSectionModal from './EditSectionModal.vue'

const props = defineProps<{
  visible: boolean
  state: OptimizeState
}>()

// 锁定背景滚动，防止滚动穿透
const isScrollLocked = useScrollLock(document.body)

// 退出二次确认状态
const showCloseConfirm = ref(false)

// 全屏对比状态
const showFullComparison = ref(false)

// ==================== 计时器 ====================
const { formatElapsed } = useStageTimer(() => props.state.isOptimizing)

// ==================== 编辑功能 ====================
const stageHistoryRef = toRef(props.state, 'stageHistory')
const {
  hasEdits,
  showEditModal,
  editingSection,
  editingItemIndex,
  handleEditSection,
  handleEditSave,
  handleEditCancel,
  resetEdits,
  getDisplayAfterSection
} = useStageEdit(stageHistoryRef)

// ==================== 监听器 ====================
watch(
  () => props.visible,
  (visible) => {
    isScrollLocked.value = visible
    if (!visible) {
      showCloseConfirm.value = false
    }
  },
  { immediate: true }
)

// ==================== 计算属性 ====================
// 是否有优化后的区块数据可应用
const hasOptimizedSections = computed(() => {
  const optimizeStage = props.state.stageHistory.find(h => h.stage === 'optimize_section')
  const data = optimizeStage?.data
  return data?.beforeSection?.length > 0 && data?.afterSection?.length > 0
})

// 全屏对比所需数据
const fullComparisonData = computed(() => {
  const optimizeStage = props.state.stageHistory.find(h => h.stage === 'optimize_section')
  if (!optimizeStage?.data) return null
  return {
    beforeSection: optimizeStage.data.beforeSection,
    afterSection: getDisplayAfterSection(optimizeStage),
    changes: optimizeStage.data.changes || [],
    improvementScore: optimizeStage.data.improvementScore,
    beforeResume: optimizeStage.data.beforeResume
  }
})

// ==================== 事件 ====================
const emit = defineEmits<{
  'update:visible': [value: boolean]
  'cancel': []
  'retry': []
  'complete': []
  'apply': [editedAfterSection: ResumeSection[] | null]
  'toggleExpand': [stage: OptimizeStage]
}>()

function handleCloseClick() {
  if (props.state.isCompleted && hasOptimizedSections.value) {
    showCloseConfirm.value = true
    return
  }
  handleClose()
}

function confirmClose() {
  showCloseConfirm.value = false
  handleClose()
}

function handleClose() {
  if (props.state.isCompleted) {
    emit('complete')
  }
  emit('update:visible', false)
}

function handleCancel() {
  emit('cancel')
}

function handleRetry() {
  emit('retry')
}

function handleApply() {
  // 获取当前编辑后的数据
  const optimizeStage = props.state.stageHistory.find(h => h.stage === 'optimize_section')
  const editedData = getDisplayAfterSection(optimizeStage || { data: {} })
  emit('apply', editedData || null)
}

function handleToggleExpand(stage: OptimizeStage) {
  emit('toggleExpand', stage)
}
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
  padding: $spacing-lg;
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

// 目标岗位
.target-info {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-lg;
  background: rgba(255, 255, 255, 0.02);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.target-label {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.target-value {
  font-size: $text-sm;
  color: $color-accent;
  font-weight: $weight-medium;
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

.close-confirm-group {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.close-confirm-text {
  font-size: $text-xs;
  color: $color-error;
  opacity: 0.8;
}

.apply-hint {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  margin-right: auto;
  font-size: $text-xs;
  color: $color-text-tertiary;

  svg {
    flex-shrink: 0;
    opacity: 0.7;
  }
}

.footer-btn {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-lg;
  font-size: $text-sm;
  font-weight: $weight-medium;
  border-radius: $radius-md;
  transition: all $transition-fast;

  &.primary {
    background: $gradient-gold;
    color: $color-bg-deep;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(212, 168, 83, 0.3);
    }
  }

  &.secondary {
    background: rgba(255, 255, 255, 0.05);
    color: $color-text-secondary;

    &:hover {
      background: rgba(255, 255, 255, 0.1);
      color: $color-text-primary;
    }
  }

  &.danger-text {
    background: transparent;
    color: $color-error;
    padding: $spacing-sm $spacing-md;

    &:hover {
      background: rgba(248, 113, 113, 0.1);
    }
  }

  &.loading {
    opacity: 0.7;
    cursor: not-allowed;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;

    &:hover {
      transform: none;
      box-shadow: none;
    }
  }

  .spinner-icon {
    animation: spin 1s linear infinite;
  }
}

// 全屏简历对比
.fullscreen-comparison-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.92);
  backdrop-filter: blur(12px);
  z-index: $z-overlay;
  display: flex;
  flex-direction: column;
}

.fullscreen-comparison-container {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
}

.fullscreen-comparison-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-md $spacing-xl;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.fullscreen-comparison-title {
  font-size: $text-lg;
  font-weight: $weight-semibold;
  color: $color-text-primary;
}

.fullscreen-comparison-close {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $color-text-tertiary;
  border-radius: $radius-sm;
  transition: all $transition-fast;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: $color-text-primary;
  }
}

.fullscreen-comparison-body {
  flex: 1;
  overflow-y: auto;
  padding: $spacing-lg $spacing-xl;
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

.fullscreen-comparison-enter-active,
.fullscreen-comparison-leave-active {
  transition: opacity 0.25s ease;
}

.fullscreen-comparison-enter-from,
.fullscreen-comparison-leave-to {
  opacity: 0;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
