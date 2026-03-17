<!--=====================================================
  LandIt 定制简历弹窗
  @author Azir
=====================================================-->

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="visible"
        class="modal-overlay"
        :class="{ 'modal-overlay--high': overlay || showComparisonView }"
        @click.self="handleClose"
      >
        <div class="modal-container" :class="{
          'modal-container--wide': showComparisonView,
          'modal-container--fullscreen': tailorState.isTailoring || tailorState.isCompleted
        }">
          <!-- 头部 -->
          <div class="modal-header">
            <h3 class="header-title">
              <svg v-if="!showComparisonView" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" y1="13" x2="8" y2="13"></line>
                <line x1="16" y1="17" x2="8" y2="17"></line>
              </svg>
              <svg v-else width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                <line x1="12" y1="3" x2="12" y2="21"></line>
              </svg>
              {{ showComparisonView ? '对比&编辑' : '定制简历' }}
            </h3>
            <button class="close-btn" @click="handleClose">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>

          <!-- 输入表单（未开始时显示） -->
          <div v-if="!tailorState.isTailoring && !tailorState.isCompleted" class="form-section">
            <div class="form-group">
              <label class="form-label required">目标职位</label>
              <input
                v-model="formData.targetPosition"
                type="text"
                class="form-input"
                placeholder="如：高级Java工程师"
              />
            </div>

            <div class="form-group">
              <label class="form-label required">职位描述</label>
              <textarea
                v-model="formData.jobDescription"
                class="form-textarea"
                placeholder="粘贴目标职位的 JD 内容..."
                rows="8"
              ></textarea>
            </div>

            <div class="form-group">
              <label class="form-label">简历名称（可选）</label>
              <input
                v-model="formData.resumeName"
                type="text"
                class="form-input"
                placeholder="如：高级Java工程师定制版"
              />
            </div>

            <div class="form-actions">
              <button class="btn-cancel" @click="handleClose">取消</button>
              <button
                class="btn-submit"
                :disabled="!isFormValid"
                @click="startTailorResume"
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon>
                </svg>
                生成定制简历
              </button>
            </div>
          </div>

          <!-- 进度展示（进行中时显示） -->
          <div v-else class="progress-section">
            <!-- 进度条 -->
            <div class="progress-bar-wrapper">
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: tailorState.progress + '%' }"></div>
              </div>
              <div class="progress-info">
                <span class="progress-text">{{ tailorState.message }}</span>
                <span class="progress-percent">{{ tailorState.progress }}%</span>
              </div>
            </div>

            <!-- 阶段列表 -->
            <div class="stages-list">
              <div
                v-for="item in sortedStageHistory"
                :key="item.stage"
                class="stage-item"
                :class="{
                  active: item.stage === tailorState.currentStage && tailorState.isTailoring,
                  completed: item.completed
                }"
              >
                <div
                  class="stage-main"
                  :class="{ clickable: item.completed && item.data }"
                  @click="item.completed && item.data && toggleStageExpanded(item.stage)"
                >
                  <div class="stage-left">
                    <div class="stage-indicator">
                      <svg v-if="item.completed" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="20 6 9 17 4 12"/>
                      </svg>
                      <div v-else-if="item.stage === tailorState.currentStage && tailorState.isTailoring" class="spinner"></div>
                      <div v-else class="dot"></div>
                    </div>
                    <span class="stage-label">{{ getTailorStageLabel(item.stage) }}</span>
                    <span v-if="item.startTime" class="stage-elapsed" :class="{ running: !item.endTime && item.stage === tailorState.currentStage && tailorState.isTailoring }">
                      {{ formatElapsed(item) }}
                    </span>
                  </div>
                  <div v-if="item.completed && item.data" class="expand-indicator">
                    <svg
                      width="16"
                      height="16"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                      :class="{ rotated: item.expanded }"
                    >
                      <polyline points="6 9 12 15 18 9"/>
                    </svg>
                  </div>
                </div>

                <!-- 展开的数据区域 -->
                <Transition name="expand">
                  <div v-if="item.expanded && item.data" class="stage-data">
                    <!-- JD 分析结果 -->
                    <div v-if="item.stage === 'analyze_jd'" class="data-content">
                      <div class="skills-section" v-if="item.data.requiredSkills?.length">
                        <div class="skills-title">必备技能</div>
                        <div class="skill-tags">
                          <span v-for="skill in item.data.requiredSkills" :key="skill" class="skill-tag required">{{ skill }}</span>
                        </div>
                      </div>
                      <div class="skills-section" v-if="item.data.preferredSkills?.length">
                        <div class="skills-title">优先技能</div>
                        <div class="skill-tags">
                          <span v-for="skill in item.data.preferredSkills" :key="skill" class="skill-tag preferred">{{ skill }}</span>
                        </div>
                      </div>
                      <div class="skills-section" v-if="item.data.keywords?.length">
                        <div class="skills-title">关键词</div>
                        <div class="skill-tags">
                          <span v-for="keyword in item.data.keywords" :key="keyword" class="skill-tag keyword">{{ keyword }}</span>
                        </div>
                      </div>
                      <div class="responsibilities-section" v-if="item.data.responsibilities?.length">
                        <div class="skills-title">职位职责</div>
                        <ul class="responsibilities-list">
                          <li v-for="(resp, idx) in item.data.responsibilities" :key="idx">{{ resp }}</li>
                        </ul>
                      </div>
                      <div class="info-grid">
                        <div class="info-item" v-if="item.data.seniorityLevel">
                          <span class="info-label">资历级别</span>
                          <span class="info-value">{{ item.data.seniorityLevel }}</span>
                        </div>
                        <div class="info-item" v-if="item.data.industryDomain">
                          <span class="info-label">行业领域</span>
                          <span class="info-value">{{ item.data.industryDomain }}</span>
                        </div>
                      </div>
                    </div>

                    <!-- 匹配分析结果 -->
                    <div v-else-if="item.stage === 'match_resume'" class="data-content">
                      <div class="match-score-display">
                        <div class="match-ring" :style="{ '--score': item.data.matchScore }">
                          <span>{{ item.data.matchScore }}</span>
                        </div>
                        <span class="match-label">匹配度</span>
                      </div>
                      <div class="match-details">
                        <div class="match-section" v-if="item.data.matchedSkills?.length">
                          <div class="match-title success">已匹配技能</div>
                          <div class="match-tags">
                            <span v-for="skill in item.data.matchedSkills" :key="skill" class="match-tag success">{{ skill }}</span>
                          </div>
                        </div>
                        <div class="match-section" v-if="item.data.missingSkills?.length">
                          <div class="match-title warning">缺失技能</div>
                          <div class="match-tags">
                            <span v-for="skill in item.data.missingSkills" :key="skill" class="match-tag warning">{{ skill }}</span>
                          </div>
                        </div>
                        <!-- 相关经验 -->
                        <div class="match-section" v-if="item.data.relevantExperiences?.length">
                          <div class="match-title info">相关经验</div>
                          <ul class="match-list">
                            <li v-for="(exp, idx) in item.data.relevantExperiences" :key="idx">{{ exp }}</li>
                          </ul>
                        </div>
                        <!-- 优化建议 -->
                        <div class="match-section" v-if="item.data.adjustmentSuggestions?.length">
                          <div class="match-title accent">优化建议</div>
                          <ul class="match-list">
                            <li v-for="(suggestion, idx) in item.data.adjustmentSuggestions" :key="idx">{{ suggestion }}</li>
                          </ul>
                        </div>
                      </div>
                    </div>

                    <!-- 定制简历结果 -->
                    <div v-else-if="item.stage === 'generate_tailored'" class="data-content">
                      <!-- 头部：左边统计，右边操作按钮 -->
                      <div class="tailor-summary">
                        <div class="tailor-summary-left">
                          <span class="tailor-improvement" v-if="item.data.improvementScore">
                            预计提升 {{ item.data.improvementScore }} 分
                          </span>
                          <span class="tailor-match" v-if="item.data.matchScore">
                            匹配度 {{ item.data.matchScore }}%
                          </span>
                        </div>
                        <button
                          v-if="canShowComparison"
                          class="comparison-btn"
                          @click="showComparisonView = true"
                        >
                          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                            <line x1="12" y1="3" x2="12" y2="21"/>
                          </svg>
                          对比&编辑
                        </button>
                      </div>
                      <!-- 定制说明 -->
                      <div class="tailor-notes" v-if="item.data.tailorNotes?.length">
                        <div class="notes-title">定制说明</div>
                        <ul class="notes-list">
                          <li v-for="(note, idx) in item.data.tailorNotes" :key="idx">{{ note }}</li>
                        </ul>
                      </div>
                    </div>
                  </div>
                </Transition>
              </div>
            </div>

            <!-- 错误信息 -->
            <div v-if="tailorState.hasError" class="error-section">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <line x1="12" y1="8" x2="12" y2="12"/>
                <line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
              <span>{{ tailorState.errorMessage }}</span>
            </div>
          </div>

          <!-- 底部操作（固定底部） -->
          <div v-if="tailorState.isTailoring || tailorState.isCompleted" class="modal-footer">
            <template v-if="tailorState.hasError">
              <button class="footer-btn secondary" @click="handleClose">退出</button>
            </template>
            <template v-else-if="tailorState.isTailoring">
              <button class="footer-btn secondary" @click="handleCancel">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"/>
                  <line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
                取消
              </button>
            </template>
            <template v-else-if="tailorState.isCompleted">
              <button class="footer-btn primary" @click="viewTailoredResume">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                  <circle cx="12" cy="12" r="3"/>
                </svg>
                查看定制简历
              </button>
            </template>
          </div>

          <!-- 对比视图（新增） -->
          <div v-if="showComparisonView" class="comparison-section">
            <ResumeComparison
              v-if="comparisonData"
              :before-section="comparisonData.beforeSection"
              :after-section="comparisonData.afterSection"
              :improvement-score="comparisonData.improvementScore"
              :editable="true"
              @edit-section="handleEditSection"
            />
            <div v-else class="comparison-empty">
              <p>暂无对比数据</p>
            </div>

            <div class="comparison-actions">
              <button class="btn-back" @click="showComparisonView = false">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="19" y1="12" x2="5" y2="12"></line>
                  <polyline points="12 19 5 12 12 5"></polyline>
                </svg>
                返回
              </button>
              <button class="btn-apply" @click="handleApplyChanges">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
                应用变更
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 编辑弹窗（复用现有组件） -->
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
import { getTailorStageLabel, type TailorComparisonData } from '@/types/resume-tailor'
import type { ResumeSection } from '@/types'
import type { ComparisonEditEvent } from '@/types/resume-optimize'
import ResumeComparison from './ResumeComparison.vue'
import EditSectionModal from './EditSectionModal.vue'

const props = withDefaults(defineProps<{
  visible: boolean
  resumeId: string
  /** 是否显示在全屏遮罩层之上 */
  overlay?: boolean
}>(), {
  overlay: false
})

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'complete': []
  'apply': [sections: ResumeSection[]]
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

// ==================== 节点运行计时器 ====================
const now = ref(Date.now())
let timerHandle: ReturnType<typeof setInterval> | null = null

// 启动/停止计时器
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

/** 格式化耗时为 mm:ss */
function formatElapsed(item: { startTime?: number; endTime?: number; stage: string }): string {
  if (!item.startTime) return ''
  const end = item.endTime ?? now.value
  const elapsed = Math.max(0, Math.floor((end - item.startTime) / 1000))
  const m = String(Math.floor(elapsed / 60)).padStart(2, '0')
  const s = String(elapsed % 60).padStart(2, '0')
  return `${m}:${s}`
}

// ===== 对比视图相关状态 =====
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

// 应用变更
function handleApplyChanges() {
  const sectionsToApply = editedAfterSection.value || comparisonData.value?.afterSection
  if (sectionsToApply) {
    emit('apply', sectionsToApply as ResumeSection[])
  }
  handleClose()
}

// 表单验证
const isFormValid = computed(() => {
  return formData.value.targetPosition.trim() && formData.value.jobDescription.trim()
})

// 固定显示所有阶段节点（与优化弹窗一致）
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
  if (!isFormValid.value || !props.resumeId) return
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

// 查看定制后的简历
function viewTailoredResume() {
  emit('complete')
  handleClose()
}

// 滚动锁定：弹窗显示时禁止背景滚动
watch(() => props.visible, (newVal) => {
  if (newVal) {
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
  }
}, { immediate: true })

// 组件销毁时确保清理滚动锁定
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

  // 高层级模式：显示在全屏遮罩层之上
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

  // 宽屏模式（对比视图）
  &.modal-container--wide {
    max-width: 95vw;
    max-height: 95vh;
  }

  // 全屏模式（SSE响应阶段）
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

.modal-header {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  padding: $spacing-lg;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);

  .header-title {
    flex: 1;
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    font-size: $text-lg;
    font-weight: $weight-semibold;
    color: $color-text-primary;

    svg {
      color: $color-accent;
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
}

// 表单样式
.form-section {
  padding: $spacing-lg;
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.form-label {
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-text-secondary;

  &.required::after {
    content: '*';
    color: $color-error;
    margin-left: 4px;
  }
}

.form-input,
.form-textarea {
  background: $color-bg-tertiary;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-md;
  padding: $spacing-md;
  color: $color-text-primary;
  font-size: $text-base;
  transition: all $transition-fast;

  &::placeholder {
    color: $color-text-tertiary;
  }

  &:focus {
    outline: none;
    border-color: $color-accent;
  }
}

.form-textarea {
  resize: vertical;
  min-height: 120px;
  font-family: inherit;
}

.form-actions {
  display: flex;
  gap: $spacing-md;
  justify-content: flex-end;
  margin-top: $spacing-md;
}

.btn-cancel,
.btn-submit {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-lg;
  font-size: $text-sm;
  font-weight: $weight-medium;
  border-radius: $radius-md;
  transition: all $transition-fast;
}

.btn-cancel {
  background: transparent;
  color: $color-text-secondary;

  &:hover {
    background: rgba(255, 255, 255, 0.05);
    color: $color-text-primary;
  }
}

.btn-submit {
  background: $gradient-gold;
  color: $color-bg-deep;

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  &:not(:disabled):hover {
    box-shadow: 0 4px 16px rgba(212, 168, 83, 0.3);
  }
}

// 进度样式
.progress-section {
  padding: $spacing-lg;
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.progress-bar-wrapper {
  .progress-bar {
    height: 8px;
    background: rgba(255, 255, 255, 0.1);
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
    font-size: $text-sm;

    .progress-text {
      color: $color-text-secondary;
    }

    .progress-percent {
      color: $color-accent;
      font-weight: $weight-medium;
    }
  }
}

// 阶段列表
.stages-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.stage-item {
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-md;
  border: 1px solid rgba(255, 255, 255, 0.05);
  overflow: hidden;

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

  svg {
    color: $color-success;
  }

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

.expand-indicator svg {
  color: $color-text-tertiary;
  transition: transform 0.2s ease;

  &.rotated {
    transform: rotate(180deg);
  }
}

// 展开数据
.stage-data {
  padding: 0 $spacing-md $spacing-md;
}

.data-content {
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-sm;
  padding: $spacing-md;
}

// 技能展示
.skills-section {
  margin-bottom: $spacing-md;

  &:last-child {
    margin-bottom: 0;
  }
}

.skills-title {
  font-size: $text-xs;
  color: $color-text-tertiary;
  margin-bottom: $spacing-sm;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.skill-tags {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-xs;
}

.skill-tag {
  padding: 2px 8px;
  font-size: $text-xs;
  border-radius: $radius-sm;

  &.required {
    background: rgba(212, 168, 83, 0.15);
    color: $color-accent;
  }

  &.preferred {
    background: rgba(96, 165, 250, 0.15);
    color: $color-info;
  }

  &.keyword {
    background: rgba(139, 92, 246, 0.15);
    color: #a78bfa;
  }
}

// 信息网格
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-md;
  margin-top: $spacing-md;
}

// 职位职责列表
.responsibilities-section {
  margin-bottom: $spacing-md;

  &:last-child {
    margin-bottom: 0;
  }
}

.responsibilities-list {
  list-style: none;
  padding: 0;
  margin: 0;

  li {
    position: relative;
    padding-left: $spacing-md;
    font-size: $text-sm;
    color: $color-text-secondary;
    margin-bottom: $spacing-xs;
    line-height: 1.5;

    &::before {
      content: '•';
      position: absolute;
      left: 0;
      color: $color-accent;
    }
  }
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.info-label {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.info-value {
  font-size: $text-sm;
  color: $color-text-primary;
}

// 匹配分数展示
.match-score-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-md;
}

.match-ring {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: conic-gradient(
    $color-accent calc(var(--score) * 3.6deg),
    rgba(255, 255, 255, 0.1) calc(var(--score) * 3.6deg)
  );
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    inset: 8px;
    background: $color-bg-tertiary;
    border-radius: 50%;
  }

  span {
    position: relative;
    z-index: 1;
    font-family: $font-display;
    font-size: $text-2xl;
    font-weight: $weight-semibold;
    color: $color-accent;
  }
}

.match-label {
  font-size: $text-sm;
  color: $color-text-secondary;
}

.match-details {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.match-section {
  .match-title {
    font-size: $text-xs;
    margin-bottom: $spacing-sm;
    text-transform: uppercase;
    letter-spacing: 0.5px;

    &.success {
      color: $color-success;
    }

    &.warning {
      color: $color-warning;
    }

    &.info {
      color: $color-info;
    }

    &.accent {
      color: $color-accent;
    }
  }

  .match-list {
    list-style: none;
    padding: 0;
    margin: 0;

    li {
      position: relative;
      padding-left: $spacing-md;
      font-size: $text-sm;
      color: $color-text-secondary;
      margin-bottom: $spacing-xs;
      line-height: 1.5;

      &::before {
        content: '•';
        position: absolute;
        left: 0;
      }
    }
  }

  // 相关经验使用蓝色圆点
  &:has(.match-title.info) .match-list li::before {
    color: $color-info;
  }

  // 优化建议使用金色圆点
  &:has(.match-title.accent) .match-list li::before {
    color: $color-accent;
  }

  .match-tags {
    display: flex;
    flex-wrap: wrap;
    gap: $spacing-xs;
  }

  .match-tag {
    padding: 2px 8px;
    font-size: $text-xs;
    border-radius: $radius-sm;

    &.success {
      background: rgba(52, 211, 153, 0.15);
      color: $color-success;
    }

    &.warning {
      background: rgba(251, 191, 36, 0.15);
      color: $color-warning;
    }
  }
}

// 定制简历结果头部
.tailor-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $spacing-md;
  padding-bottom: $spacing-md;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);

  .tailor-summary-left {
    display: flex;
    align-items: center;
    gap: $spacing-md;
  }

  .tailor-improvement {
    font-size: $text-sm;
    color: $color-success;
    font-weight: $weight-medium;
  }

  .tailor-match {
    font-size: $text-sm;
    color: $color-accent;
    font-weight: $weight-medium;
  }
}

.comparison-btn {
  display: inline-flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-xs $spacing-md;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-sm;
  font-size: $text-xs;
  color: $color-text-secondary;
  cursor: pointer;
  transition: all $transition-fast;
  white-space: nowrap;

  &:hover {
    background: rgba(212, 168, 83, 0.1);
    border-color: rgba(212, 168, 83, 0.3);
    color: $color-accent;
  }
}

// 定制说明
.tailor-notes {
  .notes-title {
    font-size: $text-xs;
    color: $color-text-tertiary;
    margin-bottom: $spacing-sm;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .notes-list {
    list-style: none;
    padding: 0;
    margin: 0;

    li {
      position: relative;
      padding-left: $spacing-lg;
      font-size: $text-sm;
      color: $color-text-secondary;
      margin-bottom: $spacing-xs;

      &::before {
        content: '✓';
        position: absolute;
        left: 0;
        color: $color-success;
      }
    }
  }
}

// 错误区域
.error-section {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-md;
  background: rgba(248, 113, 113, 0.1);
  border-radius: $radius-md;
  color: $color-error;
  font-size: $text-sm;
}

// 底部操作栏（固定底部）
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
  max-height: 500px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

// ===== 对比视图样式 =====
.comparison-section {
  padding: $spacing-lg;
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
  flex: 1;
  overflow-y: auto;
}

.comparison-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $color-text-tertiary;
}

.comparison-actions {
  display: flex;
  justify-content: space-between;
  gap: $spacing-md;
  padding-top: $spacing-md;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.btn-compare {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-lg;
  background: rgba(52, 211, 153, 0.15);
  color: $color-success;
  font-size: $text-sm;
  font-weight: $weight-medium;
  border-radius: $radius-md;
  transition: all $transition-fast;

  &:hover {
    background: rgba(52, 211, 153, 0.25);
  }
}

.btn-back {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-lg;
  background: rgba(255, 255, 255, 0.05);
  color: $color-text-secondary;
  font-size: $text-sm;
  font-weight: $weight-medium;
  border-radius: $radius-md;
  transition: all $transition-fast;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: $color-text-primary;
  }
}

.btn-apply {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-lg;
  background: $gradient-gold;
  color: $color-bg-deep;
  font-size: $text-sm;
  font-weight: $weight-medium;
  border-radius: $radius-md;
  transition: all $transition-fast;

  &:hover {
    box-shadow: 0 4px 16px rgba(212, 168, 83, 0.3);
  }
}
</style>
