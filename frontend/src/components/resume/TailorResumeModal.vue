<!--=====================================================
  CareerForge 定制简历弹窗
  @author Azir
=====================================================-->

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="visible"
        class="modal-overlay"
        :class="{ 'modal-overlay--high': overlay }"
        @click.self="handleClose"
      >
        <div
          class="modal-container"
          :class="{
            'modal-container--fullscreen': tailorState.isTailoring || tailorState.isCompleted
          }"
        >
          <!-- 头部 -->
          <ModalHeader
            :is-optimizing="tailorState.isTailoring"
            :is-completed="tailorState.isCompleted"
            :has-error="tailorState.hasError"
            custom-title="定制简历"
            @close="handleClose"
          />

          <!-- 表单（未开始时显示） -->
          <TailorForm
            v-if="!tailorState.isTailoring && !tailorState.isCompleted"
            :form-data="formData"
            @update:form-data="formData = $event"
            @submit="startTailorResume"
            @cancel="handleClose"
          />

          <!-- 进度展示（进行中/完成时显示） -->
          <template v-else>
            <!-- 定制职位 -->
            <div
              v-if="formData.targetPosition"
              class="target-info"
            >
              <span class="target-label">定制职位</span>
              <span class="target-value">{{ formData.targetPosition }}</span>
            </div>

            <!-- 进度条 -->
            <ProgressBar
              :progress="tailorState.progress"
              :message="tailorState.message"
            />

            <!-- 阶段列表 -->
            <div class="stages-list">
              <TailorStageItem
                v-for="item in sortedStageHistory"
                :key="item.stage"
                :item="item"
                :is-active="item.stage === tailorState.currentStage && tailorState.isTailoring"
                :elapsed="formatElapsed(item)"
                :can-show-comparison="canShowComparison && item.stage === 'generate_tailored'"
                @toggle-expand="toggleStageExpanded(item.stage)"
                @show-comparison="showComparisonView = true"
              />
            </div>

            <!-- 错误信息 -->
            <div
              v-if="tailorState.hasError"
              class="error-section"
            >
              <svg
                width="20"
                height="20"
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
                  y1="8"
                  x2="12"
                  y2="12"
                />
                <line
                  x1="12"
                  y1="16"
                  x2="12.01"
                  y2="16"
                />
              </svg>
              <span>{{ tailorState.errorMessage }}</span>
            </div>
          </template>

          <!-- 底部操作 -->
          <div
            v-if="tailorState.isTailoring || tailorState.isCompleted"
            class="modal-footer"
          >
            <template v-if="tailorState.hasError">
              <button
                class="footer-btn secondary"
                @click="handleClose"
              >
                退出
              </button>
            </template>
            <template v-else-if="tailorState.isTailoring">
              <button
                class="footer-btn secondary"
                @click="handleCancel"
              >
                <svg
                  width="16"
                  height="16"
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
                取消
              </button>
            </template>
            <template v-else-if="tailorState.isCompleted">
              <button
                class="footer-btn secondary"
                @click="handleClose"
              >
                不保存
              </button>
              <button
                class="footer-btn primary"
                @click="handleSaveTailor"
              >
                <svg
                  width="16"
                  height="16"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z" />
                  <polyline points="17 21 17 13 7 13 7 21" />
                  <polyline points="7 3 7 8 15 8" />
                </svg>
                保存定制简历
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
          v-if="showComparisonView"
          class="fullscreen-comparison-overlay"
        >
          <div class="fullscreen-comparison-container">
            <div class="fullscreen-comparison-header">
              <h3 class="fullscreen-comparison-title">
                简历对比
              </h3>
              <button
                class="fullscreen-comparison-close"
                @click="showComparisonView = false"
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
                v-if="comparisonData"
                :before-section="comparisonData.beforeSection"
                :after-section="comparisonData.afterSection"
                :improvement-score="comparisonData.improvementScore"
                :editable="true"
                mode="tailor"
                @edit-section="handleEditSection"
              />
              <div
                v-else
                class="comparison-empty"
              >
                <p>暂无对比数据</p>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 编辑弹窗 -->
    <EditSectionModal
      v-if="editingSection"
      :visible="!!editingSection"
      :section="editingSection.section"
      :item-index="editingSection.itemIndex"
      :overlay="true"
      @save="handleEditSave"
      @cancel="editingSection = null"
      @update:visible="editingSection = null"
    />
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue'
import { useResumeTailor } from '@/composables/useResumeTailor'
import { type TailorComparisonData } from '@/types/resume-tailor'
import type { ResumeSection } from '@/types'
import type { ComparisonEditEvent } from '@/types/resume-optimize'
import TailorForm from './tailor/TailorForm.vue'
import TailorStageItem from './tailor/TailorStageItem.vue'
import ProgressBar from './optimize/ProgressBar.vue'
import ModalHeader from './optimize/ModalHeader.vue'
import ResumeComparison from './ResumeComparison.vue'
import EditSectionModal from './EditSectionModal.vue'

const props = withDefaults(defineProps<{
  visible: boolean
  resumeId: string
  overlay?: boolean
}>(), {
  overlay: false
})

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'complete': []
  'apply': [sections: ResumeSection[]]
  'save': [data: {
    targetPosition: string
    jobDescription: string
    resumeName: string
    afterSection: ResumeSection[]
    improvementScore?: number
    matchScore?: number
    sectionRelevanceScores?: Record<string, number>
    dimensionScores?: {
      content: number
      structure: number
      matching: number
      competitiveness: number
    }
  }]
}>()

// 表单数据
const formData = ref({
  targetPosition: '',
  jobDescription: '',
  resumeName: ''
})

// 定制状态
const {
  state: tailorState,
  startTailor,
  cancelTailor,
  toggleStageExpanded
} = useResumeTailor()

// 计时器
const now = ref(Date.now())
let timerHandle: ReturnType<typeof setInterval> | null = null

watch(
  () => tailorState.isTailoring,
  (tailoring) => {
    if (tailoring) {
      timerHandle = setInterval(() => { now.value = Date.now() }, 1000)
    } else if (timerHandle) {
      clearInterval(timerHandle)
      timerHandle = null
    }
  },
  { immediate: true }
)

onUnmounted(() => {
  if (timerHandle) clearInterval(timerHandle)
})

// 格式化耗时
function formatElapsed(item: { startTime?: number; endTime?: number; stage: string }): string {
  if (!item.startTime) return ''
  const end = item.endTime ?? now.value
  const elapsed = Math.max(0, Math.floor((end - item.startTime) / 1000))
  const m = String(Math.floor(elapsed / 60)).padStart(2, '0')
  const s = String(elapsed % 60).padStart(2, '0')
  return `${m}:${s}`
}

// 对比视图相关状态
const showComparisonView = ref(false)
const editedAfterSection = ref<ResumeSection[] | null>(null)
const editingSection = ref<ComparisonEditEvent | null>(null)

// 从阶段历史获取对比数据
const comparisonData = computed<TailorComparisonData | null>(() => {
  const generateStage = tailorState.stageHistory.find(h => h.stage === 'generate_tailored')
  if (!generateStage?.data?.beforeSection || !generateStage?.data?.afterSection) {
    return null
  }
  return {
    beforeSection: generateStage.data.beforeSection,
    afterSection: editedAfterSection.value || generateStage.data.afterSection,
    improvementScore: generateStage.data.improvementScore || 0,
    matchScore: generateStage.data.matchScore || 1,
    tailorNotes: generateStage.data.tailorNotes || [],
    sectionRelevanceScores: generateStage.data.sectionRelevanceScores || {}
  }
})

// 是否可以显示对比视图
const canShowComparison = computed(() => {
  return tailorState.isCompleted && comparisonData.value !== null
})


// 初始化编辑数据
watch(comparisonData, (data) => {
  if (data && !editedAfterSection.value) {
    editedAfterSection.value = JSON.parse(JSON.stringify(data.afterSection))
  }
})

// 处理编辑
function handleEditSection(event: ComparisonEditEvent) {
  editingSection.value = event
}

function handleEditSave(data: { content: Record<string, unknown>; itemIndex?: number }) {
  if (!editedAfterSection.value || !editingSection.value) return

  const { sectionIndex, itemIndex } = editingSection.value
  const section = editedAfterSection.value[sectionIndex]

  if (itemIndex !== undefined) {
    const items = JSON.parse(section.content || '[]')
    items[itemIndex] = data.content
    section.content = JSON.stringify(items)
  } else {
    section.content = JSON.stringify(data.content)
  }

  editingSection.value = null
}

// 固定显示所有阶段节点
const sortedStageHistory = computed(() => {
  const order = ['analyze_jd', 'match_resume', 'generate_tailored'] as const
  return order.map(stage => {
    const historyItem = tailorState.stageHistory.find(h => h.stage === stage)
    return historyItem || {
      stage,
      message: '',
      timestamp: 0,
      completed: false,
      data: null,
      expanded: false,
      startTime: undefined,
      endTime: undefined
    }
  })
})

// 开始定制
function startTailorResume() {
  if (!formData.value.targetPosition.trim() || !formData.value.jobDescription.trim() || !props.resumeId) return
  startTailor(props.resumeId, formData.value.targetPosition, formData.value.jobDescription)
}

// 取消定制
function handleCancel() {
  cancelTailor()
}

// 关闭弹窗
function handleClose() {
  emit('update:visible', false)
}

// 保存定制简历
function handleSaveTailor() {
  const sectionsToSave = editedAfterSection.value || comparisonData.value?.afterSection
  if (!sectionsToSave) return

  const afterSectionData = sectionsToSave.map(section => ({
    id: section.id,
    type: section.type,
    title: section.title,
    content: typeof section.content === 'string'
      ? section.content
      : JSON.stringify(section.content),
    score: section.score
  }))

  // 获取评分数据
  const generateStage = tailorState.stageHistory.find(h => h.stage === 'generate_tailored')
  const stageData = generateStage?.data

  emit('save', {
    targetPosition: formData.value.targetPosition,
    jobDescription: formData.value.jobDescription,
    resumeName: formData.value.resumeName || `${formData.value.targetPosition}定制版`,
    afterSection: afterSectionData as ResumeSection[],
    improvementScore: stageData?.improvementScore,
    matchScore: stageData?.matchScore,
    sectionRelevanceScores: stageData?.sectionRelevanceScores,
    dimensionScores: stageData?.dimensionScores
  })
}

// 滚动锁定
watch(() => props.visible, (newVal) => {
  if (newVal) {
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
  }
}, { immediate: true })

onUnmounted(() => {
  document.body.style.overflow = ''
})
</script>

<style lang="scss" scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: $z-modal;
  padding: $spacing-lg;

  &.modal-overlay--high {
    z-index: $z-modal-overlay;
  }
}

.modal-container {
  background: $color-bg-secondary;
  border-radius: $radius-xl;
  width: 100%;
  max-width: 560px;
  max-height: 90vh;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  flex-direction: column;

  &.modal-container--fullscreen {
    width: 100%;
    height: 100%;
    max-width: 100%;
    max-height: 100%;
    background: $color-bg-primary;
    border-radius: $radius-xl;
    border: 1px solid rgba(255, 255, 255, 0.08);
  }
}

// 定制职位
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


// 阶段列表
.stages-list {
  padding: $spacing-lg;
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

// 错误区域
.error-section {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-md;
  margin: 0 $spacing-lg $spacing-lg;
  background: rgba(248, 113, 113, 0.1);
  border-radius: $radius-md;
  color: $color-error;
  font-size: $text-sm;
}

// 底部操作栏
.modal-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: $spacing-md;
  padding: $spacing-lg;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
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
      box-shadow: 0 4px 16px rgba(212, 168, 83, 0.3);
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

.comparison-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $color-text-tertiary;
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
    transform: scale(0.95) translateY(20px);
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
</style>
