<!--=====================================================
  LandIt 简历优化进度弹窗
  @author Azir
=====================================================-->

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="modal-overlay">
        <div class="modal-container">
          <!-- 头部 -->
          <div class="modal-header">
            <div class="header-icon" :class="headerIconClass">
              <svg v-if="state.isOptimizing" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 2v4m0 12v4M4.93 4.93l2.83 2.83m8.48 8.48l2.83 2.83M2 12h4m12 0h4M4.93 19.07l2.83-2.83m8.48-8.48l2.83-2.83">
                  <animateTransform attributeName="transform" type="rotate" from="0 12 12" to="360 12 12" dur="2s" repeatCount="indefinite"/>
                </path>
              </svg>
              <svg v-else-if="state.isCompleted" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                <polyline points="22 4 12 14.01 9 11.01"/>
              </svg>
              <svg v-else-if="state.hasError" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <line x1="15" y1="9" x2="9" y2="15"/>
                <line x1="9" y1="9" x2="15" y2="15"/>
              </svg>
            </div>
            <h3 class="header-title">
              {{ headerTitle }}
            </h3>
            <button class="close-btn" @click="handleClose">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>
          
          <!-- 目标岗位 -->
          <div class="target-info" v-if="state.targetPosition">
            <span class="target-label">目标岗位</span>
            <span class="target-value">{{ state.targetPosition }}</span>
          </div>
          
          <!-- 进度条 -->
          <div class="progress-section">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: state.progress + '%' }"></div>
            </div>
            <div class="progress-info">
              <span class="progress-text">{{ state.message }}</span>
              <span class="progress-percent">{{ state.progress }}%</span>
            </div>
          </div>
          
          <!-- 阶段列表 -->
          <div class="stages-section">
            <div 
              v-for="item in sortedStageHistory" 
              :key="item.stage"
              class="stage-item"
              :class="{ 
                active: item.stage === state.currentStage && state.isOptimizing,
                completed: item.completed
              }"
            >
              <div
                class="stage-main"
                :class="{ clickable: item.completed && item.data }"
                @click="item.completed && item.data && toggleExpand(item.stage)"
              >
                <div class="stage-left">
                  <div class="stage-indicator">
                    <svg v-if="item.completed" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="20 6 9 17 4 12"/>
                    </svg>
                    <div v-else-if="item.stage === state.currentStage && state.isOptimizing" class="spinner"></div>
                    <div v-else class="dot"></div>
                  </div>
                  <span class="stage-label">{{ getStageLabel(item.stage) }}</span>
                  <span v-if="item.startTime" class="stage-elapsed" :class="{ running: !item.endTime && item.stage === state.currentStage && state.isOptimizing }">
                    {{ formatElapsed(item) }}
                  </span>
                </div>
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
                    <polyline points="6 9 12 15 18 9"/>
                  </svg>
                </div>
              </div>

              <!-- 展开的数据区域 -->
              <Transition name="expand">
                <div v-if="item.expanded && item.data" class="stage-data">
                  <!-- 诊断数据 -->
                  <div v-if="item.stage === 'diagnose_quick' || item.stage === 'diagnose_precise'" class="data-content">
                    <div class="score-display">
                      <div class="score-ring" :style="{ '--score': item.data.overallScore }">
                        <span>{{ item.data.overallScore }}</span>
                      </div>
                      <span class="score-label">综合评分</span>
                    </div>
                    <div class="dimensions-grid" v-if="item.data.dimensionScores">
                      <div class="dimension-item" v-for="(dim, key) in item.data.dimensionScores" :key="key">
                        <span class="dim-name">{{ getDimensionLabel(String(key)) }}</span>
                        <span class="dim-score">{{ dim }}</span>
                      </div>
                    </div>
                    <div class="issues-section" v-if="item.data.weaknesses?.length">
                      <div class="issues-title">待改进 ({{ item.data.weaknesses.length }})</div>
                      <div
                        class="issue-item"
                        v-for="(weakness, idx) in item.data.weaknesses"
                        :key="idx"
                        :class="getWeaknessSeverity(weakness)"
                      >
                        <span class="issue-severity">
                          {{ getSeverityIcon(getWeaknessSeverity(weakness)) }}
                        </span>
                        {{ getWeaknessContent(weakness) }}
                      </div>
                    </div>
                    <div class="highlights-section" v-if="item.data.strengths?.length">
                      <div class="highlights-title">亮点</div>
                      <div class="highlight-tag" v-for="(h, idx) in item.data.strengths" :key="idx">{{ h }}</div>
                    </div>
                    <!-- 快速改进建议仅在精准诊断阶段展示 -->
                    <div class="quickwins-section" v-if="item.stage === 'diagnose_precise' && item.data.quickWins?.length">
                      <div class="quickwins-title">快速改进建议</div>
                      <div class="quickwin-item" v-for="(quickWin, idx) in item.data.quickWins" :key="idx">
                        ✓ {{ quickWin }}
                      </div>
                    </div>
                  </div>
                  
                  <!-- 生成建议数据 -->
                  <div v-else-if="item.stage === 'generate_suggestions'" class="data-content">
                    <div class="suggestions-summary">
                      <span class="suggestions-count">{{ item.data.totalSuggestions }} 条建议</span>
                    </div>
                    <div class="suggestions-list" v-if="item.data.suggestions?.length">
                      <div class="suggestion-item" v-for="(sug, idx) in item.data.suggestions" :key="idx">
                        <div class="sug-header">
                          <span class="sug-type" :class="sug.type">{{ getSuggestionTypeLabel(sug.type) }}</span>
                          <span class="sug-impact-badge" :class="sug.impact">{{ getSuggestionImpactLabel(sug.impact) }}</span>
                          <span class="sug-title">{{ sug.title }}</span>
                        </div>
                        <div class="sug-section">位置: {{ sug.position || sug.category }}</div>
                        <div class="sug-current" v-if="sug.current">当前: {{ sug.current }}</div>
                        <div class="sug-suggestion">建议：{{ sug.suggestion }}</div>
                        <div class="sug-value" v-if="sug.value">价值: {{ sug.value }}</div>
                      </div>
                    </div>
                    <div class="quickwins-section" v-if="item.data.quickWins?.length">
                      <div class="quickwins-title">快速改进项</div>
                      <div class="quickwin-item" v-for="(quickWin, idx) in item.data.quickWins" :key="idx">
                        ✓ {{ quickWin.action || quickWin }}
                      </div>
                    </div>
                  </div>
                  
                  <!-- 内容优化数据 -->
                  <div v-else-if="item.stage === 'optimize_section'" class="data-content">
                    <!-- 变更详情视图 -->
                    <div class="changes-view">
                      <div class="changes-summary">
                        <div class="changes-summary-left">
                          <span class="changes-count">{{ item.data.changeCount || item.data.changes?.length || 0 }} 处变更</span>
                          <span class="changes-improvement" v-if="item.data.improvementScore">预计提升 {{ item.data.improvementScore }} 分</span>
                        </div>
                        <button
                          v-if="item.data.beforeSection?.length || item.data.beforeResume"
                          class="comparison-btn"
                          @click="showFullComparison = true"
                        >
                          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <rect x="2" y="3" width="20" height="18" rx="2"/>
                            <line x1="12" y1="3" x2="12" y2="21"/>
                          </svg>
                          对比 & 编辑
                        </button>
                      </div>
                      <div class="confidence-badge" :class="item.data.confidence || 'medium'" v-if="item.data.confidence">
                        置信度: {{ item.data.confidence }}
                      </div>
                      <div class="changes-list" v-if="item.data.changes?.length">
                        <div class="change-item" v-for="(change, idx) in item.data.changes" :key="idx">
                          <div class="change-header">
                            <span class="change-type" :class="change.type">{{ change.typeLabel || change.type || '修改' }}</span>
                            <span class="change-field">{{ change.fieldLabel || change.field }}</span>
                          </div>
                          <div class="change-content" v-if="hasValueToShow(change)">
                            <!-- 删除类型：只显示被删除的值 -->
                            <div class="change-removed" v-if="change.type === 'removed'">
                              <ol v-if="isArray(change.beforeValue)" class="change-value-list">
                                <li v-for="(val, vIdx) in change.beforeValue" :key="vIdx">{{ val }}</li>
                              </ol>
                              <pre v-else class="change-value-text">{{ change.beforeValue }}</pre>
                            </div>

                            <!-- 新增类型：只显示新增的值 -->
                            <div class="change-added" v-else-if="change.type === 'added'">
                              <ol v-if="isArray(change.afterValue)" class="change-value-list">
                                <li v-for="(val, vIdx) in change.afterValue" :key="vIdx">{{ val }}</li>
                              </ol>
                              <pre v-else class="change-value-text">{{ change.afterValue }}</pre>
                            </div>

                            <!-- 修改类型：保持前后对比 -->
                            <template v-else>
                              <div class="change-before" v-if="change.beforeValue !== null">
                                <span class="change-label">前:</span>
                                <ol v-if="isArray(change.beforeValue)" class="change-value-list">
                                  <li v-for="(val, vIdx) in change.beforeValue" :key="vIdx">{{ val }}</li>
                                </ol>
                                <pre v-else class="change-value-text">{{ change.beforeValue }}</pre>
                              </div>
                              <div class="change-after" v-if="change.afterValue !== null">
                                <span class="change-label">后:</span>
                                <ol v-if="isArray(change.afterValue)" class="change-value-list">
                                  <li v-for="(val, vIdx) in change.afterValue" :key="vIdx">{{ val }}</li>
                                </ol>
                                <pre v-else class="change-value-text">{{ change.afterValue }}</pre>
                              </div>
                            </template>
                          </div>
                          <div class="change-reason" v-if="change.reason">{{ change.reason }}</div>
                        </div>
                      </div>
                      <div class="tips-section" v-if="item.data.tips?.length">
                        <div class="tips-title">优化提示</div>
                        <div class="tip-item" v-for="(tip, idx) in item.data.tips" :key="idx">
                          • {{ tip }}
                        </div>
                      </div>
                    </div>
                  </div>
                  
                  <!-- 默认 JSON 显示 -->
                  <div v-else class="data-content">
                    <pre class="json-display">{{ JSON.stringify(item.data, null, 2) }}</pre>
                  </div>
                </div>
              </Transition>
            </div>
          </div>
          
          <!-- 底部操作 -->
          <div class="modal-footer">
            <template v-if="state.hasError">
              <button class="footer-btn secondary" @click="handleClose">
                退出
              </button>
              <button class="footer-btn primary" @click="handleRetry">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="23 4 23 10 17 10"/>
                  <polyline points="1 20 1 14 7 14"/>
                  <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
                </svg>
                重试
              </button>
            </template>
            <template v-else-if="state.isOptimizing">
              <button class="footer-btn secondary" @click="handleCancel">
                退出
              </button>
            </template>
            <template v-else-if="state.isCompleted">
              <div v-if="showCloseConfirm" class="close-confirm-group">
                <span class="close-confirm-text">未应用的变更将丢失</span>
                <button class="footer-btn danger-text" @click="confirmClose">
                  确认退出
                </button>
                <button class="footer-btn secondary" @click="showCloseConfirm = false">
                  取消
                </button>
              </div>
              <template v-else>
                <!-- 应用变更提示 -->
                <div v-if="hasOptimizedSections && !state.isApplying" class="apply-hint">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"/>
                    <line x1="12" y1="16" x2="12" y2="12"/>
                    <line x1="12" y1="8" x2="12.01" y2="8"/>
                  </svg>
                  <span>应用后将重新进行 AI 评分</span>
                </div>
                <button v-if="hasEdits" class="footer-btn secondary" @click="resetEdits">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="1 4 1 10 7 10"></polyline>
                    <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"></path>
                  </svg>
                  重置
                </button>
                <button class="footer-btn secondary" @click="handleCloseClick">
                  退出
                </button>
                <button
                  class="footer-btn primary"
                  :class="{ loading: state.isApplying }"
                  :disabled="state.isApplying || !hasOptimizedSections"
                  @click="handleApply"
                >
                  <svg v-if="state.isApplying" class="spinner-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10" stroke-opacity="0.3"/>
                    <path d="M12 2a10 10 0 0 1 10 10"/>
                  </svg>
                  {{ state.isApplying ? '评分中...' : '应用变更' }}
                </button>
              </template>
            </template>
            <template v-else>
              <button class="footer-btn secondary" @click="handleClose">
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
        <div v-if="showFullComparison" class="fullscreen-comparison-overlay">
          <div class="fullscreen-comparison-container">
            <div class="fullscreen-comparison-header">
              <h3 class="fullscreen-comparison-title">简历对比</h3>
              <button class="fullscreen-comparison-close" @click="showFullComparison = false">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"/>
                  <line x1="6" y1="6" x2="18" y2="18"/>
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
      @save="handleEditSave"
      @cancel="handleEditCancel"
    />
  </Teleport>
</template>

<script setup lang="ts">
import { computed, ref, watch, onUnmounted } from 'vue'
import { useScrollLock } from '@vueuse/core'
import type { OptimizeState, OptimizeStage, StageHistoryItem, ResumeSection, ComparisonEditEvent, ChangeItem } from '@/types/resume-optimize'
import { getStageLabel, getDimensionLabel } from '@/types/resume-optimize'
import ResumeComparison from './ResumeComparison.vue'
import EditSectionModal from './EditSectionModal.vue'

const props = defineProps<{
  visible: boolean
  state: OptimizeState
}>()

// 锁定背景滚动，防止滚动穿透
const isScrollLocked = useScrollLock(document.body)

// ==================== 节点运行计时器 ====================
const now = ref(Date.now())
let timerHandle: ReturnType<typeof setInterval> | null = null

// 启动/停止计时器
watch(
  () => props.state.isOptimizing,
  (optimizing) => {
    if (optimizing) {
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
function formatElapsed(item: StageHistoryItem): string {
  if (!item.startTime) return ''
  const end = item.endTime ?? now.value
  const elapsed = Math.max(0, Math.floor((end - item.startTime) / 1000))
  const m = String(Math.floor(elapsed / 60)).padStart(2, '0')
  const s = String(elapsed % 60).padStart(2, '0')
  return `${m}:${s}`
}

// 退出二次确认状态
const showCloseConfirm = ref(false)

// 监听弹窗显示状态，同步锁定/解锁背景滚动
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

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'cancel': []
  'retry': []
  'complete': []
  'apply': [editedAfterSection: ResumeSection[] | null]
  'toggleExpand': [stage: OptimizeStage]
}>()

// ==================== 编辑功能 ====================

// 可编辑的 afterSection 副本（优化内容阶段的数据）
const editedAfterSection = ref<ResumeSection[] | null>(null)

// 原始 afterSection 的深拷贝（用于比较是否有编辑）
const originalAfterSection = ref<string>('')

// 编辑弹窗状态
const showEditModal = ref(false)
const editingSection = ref<ResumeSection | null>(null)
const editingSectionIndex = ref<number | null>(null)
const editingItemIndex = ref<number | null>(null)

// 监听 stageHistory 变化，初始化编辑数据
watch(
  () => props.state.stageHistory,
  (history) => {
    const optimizeStage = history.find(h => h.stage === 'optimize_section')
    if (optimizeStage?.data?.afterSection) {
      // 深拷贝数据
      const afterSectionCopy = JSON.parse(JSON.stringify(optimizeStage.data.afterSection))
      editedAfterSection.value = afterSectionCopy
      originalAfterSection.value = JSON.stringify(afterSectionCopy)
    }
  },
  { deep: true, immediate: true }
)

// 是否有编辑
const hasEdits = computed(() => {
  if (!editedAfterSection.value) return false
  return JSON.stringify(editedAfterSection.value) !== originalAfterSection.value
})

// 获取当前显示的 afterSection（可能是编辑后的）
function getDisplayAfterSection(item: { data?: { afterSection?: ResumeSection[] } }): ResumeSection[] | undefined {
  if (editedAfterSection.value) {
    return editedAfterSection.value
  }
  return item.data?.afterSection
}

// 处理编辑事件
function handleEditSection(payload: ComparisonEditEvent) {
  if (!editedAfterSection.value) return

  editingSectionIndex.value = payload.sectionIndex
  editingItemIndex.value = payload.itemIndex ?? null

  // 获取要编辑的区块
  const section = editedAfterSection.value[payload.sectionIndex]
  if (!section) return

  // 直接传完整 section，让 EditSectionModal 根据 itemIndex 自行解析
  editingSection.value = JSON.parse(JSON.stringify(section))

  showEditModal.value = true
}

// 保存编辑
function handleEditSave(data: { content: Record<string, unknown>; itemIndex?: number }) {
  if (!editedAfterSection.value || editingSectionIndex.value === null) return

  const section = editedAfterSection.value[editingSectionIndex.value]

  if (editingItemIndex.value !== null && editingItemIndex.value !== undefined) {
    // 更新聚合类型中的特定项
    const items = JSON.parse(section.content || '[]')
    items[editingItemIndex.value] = data.content
    section.content = JSON.stringify(items)
  } else {
    // 更新整个内容（单对象类型）
    section.content = JSON.stringify(data.content)
  }

  showEditModal.value = false
  editingSection.value = null
  editingSectionIndex.value = null
  editingItemIndex.value = null
}

// 取消编辑
function handleEditCancel() {
  showEditModal.value = false
  editingSection.value = null
  editingSectionIndex.value = null
  editingItemIndex.value = null
}

// 重置编辑
function resetEdits() {
  const optimizeStage = props.state.stageHistory.find(h => h.stage === 'optimize_section')
  if (optimizeStage?.data?.afterSection) {
    editedAfterSection.value = JSON.parse(JSON.stringify(optimizeStage.data.afterSection))
  }
}

// 头部图标类
const headerIconClass = computed(() => ({
  'optimizing': props.state.isOptimizing,
  'completed': props.state.isCompleted,
  'error': props.state.hasError
}))

// 头部标题
const headerTitle = computed(() => {
  if (props.state.isOptimizing) return 'AI 简历优化中...'
  if (props.state.isCompleted) return '优化完成'
  if (props.state.hasError) return '优化失败'
  return '简历优化'
})

// 排序后的阶段历史
const sortedStageHistory = computed(() => {
  const order: OptimizeStage[] = [
    'diagnose_quick',
    'generate_suggestions',
    'optimize_section'
  ]

  return order.map(stage => {
    const historyItem = props.state.stageHistory.find(h => h.stage === stage)
    return historyItem || {
      stage,
      message: '',
      timestamp: 0,
      completed: false,
      data: null,
      expanded: false
    }
  })
})

// 是否有优化后的区块数据可应用
const hasOptimizedSections = computed(() => {
  const optimizeStage = props.state.stageHistory.find(h => h.stage === 'optimize_section')
  const data = optimizeStage?.data
  return data?.beforeSection?.length > 0 && data?.afterSection?.length > 0
})

function handleCloseClick() {
  // 优化完成且有未应用的优化数据时，需要二次确认
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
  // 优化完成时触发 complete 事件，让父组件刷新数据
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
  // 传递编辑后的数据（如果有编辑的话）
  emit('apply', editedAfterSection.value)
}

function toggleExpand(stage: OptimizeStage) {
  emit('toggleExpand', stage)
}

// 全屏简历对比状态
const showFullComparison = ref(false)

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

// 严重性图标映射
const SEVERITY_ICONS: Record<string, string> = {
  high: '⚠️',
  medium: '💡',
  low: '✨'
}

function getSeverityIcon(severity: string): string {
  return SEVERITY_ICONS[severity] || '•'
}

function getWeaknessContent(weakness: string | { content: string }): string {
  return typeof weakness === 'string' ? weakness : weakness.content
}

function getWeaknessSeverity(weakness: string | { severity?: string }): string {
  return typeof weakness === 'string' ? 'medium' : (weakness.severity || 'medium')
}

// 建议类型标签映射
const SUGGESTION_TYPE_LABELS: Record<string, string> = {
  critical: '严重',
  improvement: '改进',
  enhancement: '优化'
}

function getSuggestionTypeLabel(type: string): string {
  return SUGGESTION_TYPE_LABELS[type] || type
}

// 建议影响程度标签映射
const SUGGESTION_IMPACT_LABELS: Record<string, string> = {
  high: '高影响',
  medium: '中影响',
  low: '低影响'
}

function getSuggestionImpactLabel(impact: string): string {
  return SUGGESTION_IMPACT_LABELS[impact] || impact
}

// 判断是否为数组
function isArray(value: unknown): value is string[] {
  return Array.isArray(value)
}

/**
 * 判断变更项是否有值需要展示
 * 删除类型需要 beforeValue，新增类型需要 afterValue，修改类型需要至少一个有值
 */
function hasValueToShow(change: ChangeItem): boolean {
  if (change.type === 'removed') {
    return change.beforeValue !== null
  }
  if (change.type === 'added') {
    return change.afterValue !== null
  }
  return change.beforeValue !== null || change.afterValue !== null
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

// 头部
.modal-header {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  padding: $spacing-lg;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.header-icon {
  width: 40px;
  height: 40px;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  
  &.optimizing {
    background: rgba(212, 168, 83, 0.15);
    color: $color-accent;
  }
  
  &.completed {
    background: rgba(52, 211, 153, 0.15);
    color: $color-success;
  }
  
  &.error {
    background: rgba(248, 113, 113, 0.15);
    color: $color-error;
  }
}

.header-title {
  flex: 1;
  font-size: $text-lg;
  font-weight: $weight-semibold;
  color: $color-text-primary;
}

.close-btn {
  width: 32px;
  height: 32px;
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

// 进度条
.progress-section {
  padding: $spacing-lg;
}

.progress-bar {
  height: 6px;
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
  align-items: center;
}

.progress-text {
  font-size: $text-sm;
  color: $color-text-secondary;
}

.progress-percent {
  font-size: $text-sm;
  font-weight: $weight-semibold;
  color: $color-accent;
}

// 阶段列表
.stages-section {
  flex: 1;
  overflow-y: auto;
  padding: 0 $spacing-lg $spacing-lg;
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
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
.stage-data {
  margin-top: $spacing-md;
  padding-top: $spacing-md;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.data-content {
  font-size: $text-sm;
}

.data-row {
  display: flex;
  justify-content: space-between;
  padding: $spacing-xs 0;
}

.data-label {
  color: $color-text-tertiary;
}

.data-value {
  color: $color-text-primary;
  font-weight: $weight-medium;
}

.modules-list {
  margin-top: $spacing-sm;
}

.modules-title {
  font-size: $text-xs;
  color: $color-text-tertiary;
  margin-bottom: $spacing-xs;
}

.module-tag {
  display: inline-block;
  padding: 2px 8px;
  margin: 2px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: $radius-sm;
  font-size: $text-xs;
  color: $color-text-secondary;
}

// 诊断数据样式
.score-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: $spacing-md;
}

.score-ring {
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
    background: $color-bg-secondary;
    border-radius: 50%;
  }
  
  span {
    position: relative;
    z-index: 1;
    font-family: $font-display;
    font-size: $text-2xl;
    font-weight: $weight-bold;
    color: $color-accent;
  }
}

.score-label {
  margin-top: $spacing-xs;
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.dimensions-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-xs;
  margin-bottom: $spacing-md;
}

.dimension-item {
  display: flex;
  justify-content: space-between;
  padding: $spacing-xs $spacing-sm;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-sm;
}

.dim-name {
  font-size: $text-xs;
  color: $color-text-tertiary;
}

.dim-score {
  font-size: $text-xs;
  font-weight: $weight-semibold;
  color: $color-accent;
}

.issues-section {
  margin-bottom: $spacing-sm;
}

.issues-title,
.highlights-title {
  font-size: $text-xs;
  color: $color-text-tertiary;
  margin-bottom: $spacing-xs;
}

.issue-item {
  display: flex;
  align-items: flex-start;
  gap: $spacing-xs;
  padding: $spacing-xs;
  font-size: $text-xs;
  color: $color-text-secondary;
  
  &.high {
    color: $color-error;
  }
  &.medium {
    color: $color-warning;
  }
}

.issue-severity {
  flex-shrink: 0;
}

.highlight-tag {
  display: inline-block;
  padding: 2px 8px;
  margin: 2px;
  background: rgba(52, 211, 153, 0.1);
  color: $color-success;
  border-radius: $radius-sm;
  font-size: $text-xs;
}

.quickwins-section {
  margin-top: $spacing-sm;
}

.quickwins-title {
  font-size: $text-xs;
  color: $color-text-tertiary;
  margin-bottom: $spacing-xs;
}

.quickwin-item {
  display: flex;
  align-items: flex-start;
  gap: $spacing-xs;
  padding: $spacing-xs;
  font-size: $text-xs;
  color: $color-text-secondary;
  background: rgba(251, 191, 36, 0.05);
  border-radius: $radius-sm;
  margin-bottom: 4px;
}

// 建议数据样式
.suggestions-summary {
  display: flex;
  justify-content: space-between;
  margin-bottom: $spacing-sm;
}

.suggestions-count {
  font-size: $text-sm;
  color: $color-text-primary;
  font-weight: $weight-medium;
}

.suggestions-improvement {
  font-size: $text-xs;
  color: $color-success;
}

.suggestion-item {
  padding: $spacing-sm;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-sm;
  margin-bottom: $spacing-xs;
}

.sug-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: 4px;
}

.sug-priority {
  padding: 2px 6px;
  border-radius: $radius-sm;
  font-size: $text-xs;
  text-transform: uppercase;

  &.high {
    background: rgba(248, 113, 113, 0.15);
    color: $color-error;
  }
  &.medium {
    background: rgba(251, 191, 36, 0.15);
    color: $color-warning;
  }
  &.low {
    background: rgba(96, 165, 250, 0.15);
    color: $color-info;
  }
}

// 建议类型标签
.sug-type {
  padding: 2px 6px;
  border-radius: $radius-sm;
  font-size: $text-xs;

  &.critical {
    background: rgba(248, 113, 113, 0.15);
    color: $color-error;
  }
  &.improvement {
    background: rgba(251, 191, 36, 0.15);
    color: $color-warning;
  }
  &.enhancement {
    background: rgba(96, 165, 250, 0.15);
    color: $color-info;
  }
}

// 建议影响程度标签
.sug-impact-badge {
  padding: 2px 6px;
  border-radius: $radius-sm;
  font-size: $text-xs;

  &.high {
    background: rgba(248, 113, 113, 0.15);
    color: $color-error;
  }
  &.medium {
    background: rgba(251, 191, 36, 0.15);
    color: $color-warning;
  }
  &.low {
    background: rgba(96, 165, 250, 0.15);
    color: $color-info;
  }
}

.sug-title {
  font-size: $text-sm;
  color: $color-text-primary;
}

.sug-section {
  font-size: $text-xs;
  color: $color-text-tertiary;
  margin-bottom: 4px;
}

.sug-current {
  font-size: $text-xs;
  color: $color-error;
  margin-bottom: 4px;
  padding-left: $spacing-sm;
}

.sug-suggestion {
  font-size: $text-xs;
  color: $color-text-primary;
  margin-bottom: 4px;
  padding-left: $spacing-sm;
  line-height: 1.4;
}

.sug-impact {
  font-size: $text-xs;
  color: $color-success;
  padding-left: $spacing-sm;
  font-style: italic;
}

.sug-value {
  font-size: $text-xs;
  color: $color-success;
  padding-left: $spacing-sm;
  font-style: italic;
}

// 变更数据样式
.changes-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-sm;
}

.changes-summary-left {
  display: flex;
  align-items: center;
  gap: $spacing-md;
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

.changes-view {
  margin-top: $spacing-md;
}

// 全屏简历对比
.fullscreen-comparison-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.92);
  backdrop-filter: blur(12px);
  z-index: 2000;
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

// 全屏对比动画
.fullscreen-comparison-enter-active,
.fullscreen-comparison-leave-active {
  transition: opacity 0.25s ease;
}

.fullscreen-comparison-enter-from,
.fullscreen-comparison-leave-to {
  opacity: 0;
}

.changes-count {
  font-size: $text-sm;
  color: $color-text-primary;
  font-weight: $weight-medium;
}

.changes-improvement {
  font-size: $text-xs;
  color: $color-success;
}

.confidence-badge {
  display: inline-block;
  padding: 2px 8px;
  margin-bottom: $spacing-sm;
  border-radius: $radius-sm;
  font-size: $text-xs;
  
  &.high {
    background: rgba(52, 211, 153, 0.15);
    color: $color-success;
  }
  &.medium {
    background: rgba(251, 191, 36, 0.15);
    color: $color-warning;
  }
  &.low {
    background: rgba(248, 113, 113, 0.15);
    color: $color-error;
  }
}

.change-item {
  padding: $spacing-sm;
  background: rgba(255, 255, 255, 0.02);
  border-radius: $radius-sm;
  margin-bottom: $spacing-xs;
}

.change-header {
  display: flex;
  gap: $spacing-sm;
  margin-bottom: 4px;
}

.change-type {
  padding: 2px 6px;
  border-radius: $radius-sm;
  font-size: 10px;
  text-transform: uppercase;

  &.modified {
    background: rgba(96, 165, 250, 0.15);
    color: $color-info;
  }
  &.added {
    background: rgba(52, 211, 153, 0.15);
    color: $color-success;
  }
  &.removed {
    background: rgba(248, 113, 113, 0.15);
    color: $color-error;
  }
}

.change-field {
  font-size: $text-xs;
  color: $color-accent;
}

.change-content {
  margin-bottom: 4px;
}

.change-label {
  font-size: $text-xs;
  font-weight: $weight-medium;
  margin-right: $spacing-xs;
  opacity: 0.7;
}

.change-before,
.change-after {
  font-size: $text-xs;
  padding: $spacing-xs;
  border-radius: $radius-sm;
  margin-bottom: 4px;
}

.change-before {
  color: $color-error;
  background: rgba(248, 113, 113, 0.05);
}

.change-after {
  color: $color-success;
  background: rgba(52, 211, 153, 0.05);
}

// 删除类型和新增类型的单值展示样式
.change-removed,
.change-added {
  font-size: $text-xs;
  padding: $spacing-xs;
  border-radius: $radius-sm;
  margin-bottom: 4px;
}

.change-removed {
  color: $color-error;
  background: rgba(248, 113, 113, 0.05);
}

.change-added {
  color: $color-success;
  background: rgba(52, 211, 153, 0.05);
}

// 值列表样式（使用数字序号）
.change-value-list {
  padding: 0 0 0 $spacing-lg;
  list-style: decimal;

  li {
    padding: 2px 0;
    font-size: $text-xs;
    line-height: 1.5;

    &::marker {
      color: inherit;
      opacity: 0.6;
    }
  }
}

// 值文本样式（保留换行）
.change-value-text {
  margin: $spacing-xs 0 0 0;
  padding: $spacing-xs;
  font-size: $text-xs;
  font-family: inherit;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
  background: rgba(0, 0, 0, 0.1);
  border-radius: $radius-sm;
}

.change-reason {
  font-size: $text-xs;
  color: $color-text-tertiary;
  font-style: italic;
}

.tips-section {
  margin-top: $spacing-sm;
  padding-top: $spacing-sm;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.tips-title {
  font-size: $text-xs;
  color: $color-text-tertiary;
  margin-bottom: $spacing-xs;
}

.tip-item {
  font-size: $text-xs;
  color: $color-text-secondary;
  padding: $spacing-xs;
  line-height: 1.4;
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
  
  &.danger {
    background: rgba(248, 113, 113, 0.1);
    color: $color-error;

    &:hover {
      background: rgba(248, 113, 113, 0.2);
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
</style>
