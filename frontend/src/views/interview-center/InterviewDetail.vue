<template>
  <div class="interview-detail-page" v-if="!loading && interview">
    <header class="page-header">
      <div class="header-left">
        <button class="back-btn" @click="goBack">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M19 12H5"></path>
            <polyline points="12 19 5 12 12 5"></polyline>
          </svg>
        </button>
        <div class="header-title">
          <span class="company-name">{{ interview.companyName }}</span>
          <span class="separator">/</span>
          <span class="position-name">{{ interview.position }}</span>
          <span v-if="interview.roundType" class="round-badge">{{ getRoundLabel(interview) }}</span>
        </div>
      </div>
      <div class="header-actions">
        <button
          class="btn btn-mock-interview"
          :disabled="!canStartMockInterview"
          :title="mockInterviewHint"
          @click="startMockInterview"
        >
          🎙️ 模拟面试
        </button>
        <button class="btn btn-secondary" @click="showEditDialog = true">编辑</button>
        <button class="btn btn-danger" @click="handleDelete" title="删除面试">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="3 6 5 6 21 6"></polyline>
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
          </svg>
        </button>
      </div>
    </header>

    <div class="detail-content">
      <!-- 弹窗组件 -->
      <AddPreparationDialog
        v-if="showAddPreparationDialog"
        :interview-id="interview.id"
        @close="showAddPreparationDialog = false"
        @added="handlePreparationAdded"
      />
      <ReviewNoteDialog
        v-if="showReviewDialog"
        :interview-id="interview.id"
        :existing-note="interview.reviewNote"
        @close="showReviewDialog = false"
        @saved="handleReviewSaved"
      />
      <EditInterviewDialog
        v-if="showEditDialog"
        :interview="interview"
        @close="showEditDialog = false"
        @saved="handleInterviewUpdated"
      />

      <!-- AI 生成进度弹窗 -->
      <PreparationProgressModal
        v-model:visible="showPrepModal"
        :state="preparationState"
        :interview="interview"
        @save="handleSavePreparationItems"
        @cancel="handleCancelPreparation"
        @retry="handleRetryPreparation"
      />

      <!-- 模拟面试配置弹窗 -->
      <MockInterviewConfigDialog
        v-if="showMockConfigDialog"
        @close="showMockConfigDialog = false"
        @submit="handleMockConfigSubmit"
      />

      <!-- 麦克风权限弹窗 -->
      <MicrophonePermissionDialog
        v-if="showPermissionDialog"
        :state="permissionDialogState"
        :error-message="permissionErrorMessage"
        @close="handlePermissionDialogClose"
        @retry="handlePermissionRetry"
        @retryAfterSettings="handlePermissionRetry"
      />

      <!-- 删除确认弹窗 -->
      <ConfirmModal
        :visible="confirmVisible"
        :title="confirmTitle"
        :message="confirmMessage"
        :confirm-text="confirmText"
        :danger="confirmDanger"
        @confirm="handleConfirm"
        @cancel="handleCancel"
      />

      <!-- 面试信息卡片 -->
      <section class="interview-info-card">
        <!-- 面试时间单独一行 -->
        <div class="info-row">
          <span class="info-item">
            <span class="info-icon">📅</span>
            <span class="info-label">面试时间：</span>
            <span class="info-value">{{ formatDateTime(interview.interviewDate) }}</span>
          </span>
        </div>
        <!-- 面试类型和相关信息 -->
        <div class="info-row">
          <span class="info-item">
            <span class="info-icon">💻</span>
            <span class="info-label">面试类型：</span>
            <span class="info-value" v-if="interview.interviewType">{{ getInterviewTypeLabel(interview.interviewType) }}</span>
            <span class="info-value" v-else>未设置</span>
          </span>
          <template v-if="interview.interviewType === 'onsite' && interview.location">
            <span class="info-item">
              <span class="info-icon">📍</span>
              <span class="info-label">地点：</span>
              <span class="info-value">{{ interview.location }}</span>
            </span>
          </template>
          <template v-if="interview.interviewType === 'online' && interview.onlineLink">
            <span class="info-item">
              <span class="info-icon">🔗</span>
              <span class="info-label">会议链接：</span>
              <a :href="interview.onlineLink" target="_blank" class="link-value">{{ interview.onlineLink }}</a>
              <button class="copy-btn" @click="copyToClipboard(interview.onlineLink)">复制</button>
            </span>
          </template>
          <template v-if="interview.interviewType === 'online' && interview.meetingPassword">
            <span class="info-item">
              <span class="info-icon">🔑</span>
              <span class="info-label">会议密码：</span>
              <span class="info-value">{{ interview.meetingPassword }}</span>
              <button class="copy-btn" @click="copyToClipboard(interview.meetingPassword)">复制</button>
            </span>
          </template>
        </div>
        <div class="info-row">
          <span class="info-item">
            <span class="info-icon">📊</span>
            <span class="info-label">状态：</span>
            <select
              class="status-select"
              :class="interview.status"
              :value="interview.status"
              @change="handleStatusChange(($event.target as HTMLSelectElement).value)"
            >
              <option v-for="(label, key) in statusOptions" :key="key" :value="key">
                {{ label }}
              </option>
            </select>
          </span>
          <span class="info-item">
            <span class="info-icon">🎯</span>
            <span class="info-label">最终结果：</span>
            <select
              class="result-select"
              :value="interview.overallResult || ''"
              @change="handleResultChange(($event.target as HTMLSelectElement).value)"
            >
              <option value="">未设置</option>
              <option v-for="(label, key) in resultOptions" :key="key" :value="key">
                {{ label }}
              </option>
            </select>
          </span>
        </div>

        <!-- 关联简历 -->
        <div class="info-row" v-if="interview.resumeId">
          <span class="info-item">
            <span class="info-icon">📄</span>
            <span class="info-label">关联简历：</span>
            <router-link
              :to="`/resume/${interview.resumeId}`"
              class="resume-link"
            >
              {{ interview.resumeName || '查看简历' }}
            </router-link>
          </span>
        </div>

        <!-- 备注 -->
        <div class="info-row" v-if="interview.notes">
          <span class="info-item notes-item">
            <span class="info-icon">📝</span>
            <span class="info-label">备注：</span>
            <span class="info-value notes-content">{{ interview.notes }}</span>
          </span>
        </div>

        <!-- 职位详情折叠区域 -->
        <div
          class="position-detail-toggle"
          v-if="interview.jdContent || interview.companyResearch || interview.jdAnalysis"
        >
          <button class="toggle-btn" @click="showPositionDetail = !showPositionDetail">
            <svg
              :class="{ rotated: showPositionDetail }"
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <polyline points="6 9 12 15 18 9"></polyline>
            </svg>
            <span>{{ showPositionDetail ? '收起职位详情' : '展开职位详情' }}</span>
          </button>
        </div>

        <div class="position-detail-content" v-if="showPositionDetail">
          <div v-if="interview.jdContent" class="detail-item">
            <h4>职位描述 (JD)</h4>
            <div class="detail-text">{{ interview.jdContent }}</div>
          </div>
          <div v-if="interview.companyResearch" class="detail-item">
            <h4>公司调研</h4>
            <div class="detail-text">{{ interview.companyResearch }}</div>
          </div>
          <div v-if="interview.jdAnalysis" class="detail-item">
            <h4>JD 分析</h4>
            <div class="detail-text">{{ interview.jdAnalysis }}</div>
          </div>
        </div>
      </section>

      <section class="preparations-section">
        <div class="section-header">
          <h2>准备清单</h2>
          <div class="header-actions">
            <button
              class="btn btn-sm btn-ai"
              @click="handleAIGeneratePreparation"
              :disabled="preparationState.isRunning"
            >
              <span v-if="preparationState.isRunning">生成中...</span>
              <span v-else>AI 生成</span>
            </button>
            <button class="btn btn-sm btn-secondary" @click="showAddPreparationDialog = true">+ 添加事项</button>
          </div>
        </div>

        <!-- 进度条 -->
        <PreparationProgress
          v-if="interview.preparations && interview.preparations.length > 0"
          :preparations="interview.preparations"
        />

        <!-- 分组展示准备事项 -->
        <div class="preparations-groups">
          <PreparationGroup
            v-for="groupType in sortedGroupTypes"
            :key="groupType"
            :group-type="groupType"
            :preparations="groupedPreparations[groupType]"
            @toggle="togglePreparation"
            @delete="handleDeletePreparation"
          />
          <div v-if="!interview.preparations || interview.preparations.length === 0" class="empty-preparations">
            <p>暂无准备事项，点击"AI 生成"或"+ 添加事项"开始准备</p>
          </div>
        </div>
      </section>

      <section class="review-section">
        <div class="section-header">
          <h2>复盘笔记</h2>
        </div>

        <!-- 面试过程输入区域（支持音频上传 + 文本输入） -->
        <AudioUploadArea
          v-if="interview"
          :interview-id="interview.id"
          v-model="sessionTranscript"
          @save="handleSaveTranscript"
        />

        <!-- AI 分析入口区域 -->
        <div class="ai-analysis-entry">
          <div class="entry-left">
            <span class="entry-label">🤖 AI 分析</span>
            <span class="entry-hint">{{ aiAnalysisEntryHint }}</span>
          </div>
          <div class="tooltip-wrapper" v-if="!canStartAIAnalysis && !reviewState.isRunning">
            <button class="btn btn-sm btn-ai" disabled>
              <span>开始分析</span>
            </button>
            <span class="tooltip-text">{{ aiAnalysisHint }}</span>
          </div>
          <button
            v-else
            class="btn btn-sm btn-ai"
            @click="handleAIAnalysis"
            :disabled="reviewState.isRunning"
          >
            <span v-if="reviewState.isRunning">分析中...</span>
            <span v-else>开始分析</span>
          </button>
        </div>

        <!-- AI 分析进度 -->
        <div v-if="reviewState.isRunning" class="progress-indicator">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: reviewState.progress + '%' }"></div>
          </div>
          <p class="progress-message">{{ reviewState.message }}</p>
        </div>

        <!-- AI 分析卡片 -->
        <AIAnalysisCard
          v-if="interview.aiAnalysisNote || (reviewState.isCompleted && reviewState.adviceList.length > 0)"
          :ai-analysis-note="interview.aiAnalysisNote"
          :is-analyzing="reviewState.isRunning"
          :default-expanded="reviewState.isCompleted && reviewState.adviceList.length > 0"
          :show-reference-button="!interview.reviewNote"
          @reanalyze="handleReanalyze"
          @reference="handleReferenceAIAdvice"
        />

        <!-- 分隔线 -->
        <div v-if="interview.aiAnalysisNote && interview.reviewNote" class="section-divider"></div>

        <!-- 手动复盘笔记 -->
        <div v-if="interview.reviewNote" class="review-note">
          <div class="note-header">
            <span class="note-icon">📋</span>
            <span class="note-title">我的复盘笔记</span>
            <button class="btn btn-sm btn-edit-note" @click="showReviewDialog = true">编辑</button>
          </div>
          <div class="note-section">
            <h4>整体感受</h4>
            <p>{{ interview.reviewNote.overallFeeling || '暂无' }}</p>
          </div>
          <div class="note-section">
            <h4>好的方面</h4>
            <p>{{ interview.reviewNote.highPoints || '暂无' }}</p>
          </div>
          <div class="note-section">
            <h4>不足之处</h4>
            <p>{{ interview.reviewNote.weakPoints || '暂无' }}</p>
          </div>
          <div class="note-section">
            <h4>经验教训</h4>
            <p>{{ interview.reviewNote.lessonsLearned || '暂无' }}</p>
          </div>
        </div>
        <!-- 添加复盘笔记按钮（只要没有手动笔记就显示） -->
        <div v-else class="empty-review">
          <button class="btn btn-primary" @click="showReviewDialog = true">
            + 添加复盘笔记
          </button>
        </div>
      </section>
    </div>
  </div>

  <div v-else class="loading-state">
    <p>加载中...</p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getInterviewDetail,
  togglePreparationComplete,
  deletePreparation as deletePreparationApi,
  deleteInterview,
  updateInterview,
  batchAddPreparations
} from '@/api/interview-center'
import { createSession } from '@/api/interview-voice'
import {
  INTERVIEW_STATUS_LABELS,
  INTERVIEW_RESULT_LABELS,
  ROUND_TYPE_LABELS,
  INTERVIEW_TYPE_LABELS,
  type InterviewDetail,
  type InterviewStatus,
  type InterviewResult,
  type InterviewType,
  type PreparationVO,
  type AdviceItem
} from '@/types/interview-center'
import { useNotificationStore } from '@/stores/notification'
// 弹窗组件
import AddPreparationDialog from '@/components/interview-center/AddPreparationDialog.vue'
import ReviewNoteDialog from '@/components/interview-center/ReviewNoteDialog.vue'
import EditInterviewDialog from '@/components/interview-center/EditInterviewDialog.vue'
import PreparationProgressModal from '@/components/interview-center/PreparationProgressModal.vue'
import MockInterviewConfigDialog from '@/components/interview-center/MockInterviewConfigDialog.vue'
import MicrophonePermissionDialog from '@/components/interview-center/MicrophonePermissionDialog.vue'
// 新增组件
import PreparationProgress from '@/components/interview-center/PreparationProgress.vue'
import PreparationGroup from '@/components/interview-center/PreparationGroup.vue'
import AudioUploadArea from '@/components/interview-center/AudioUploadArea.vue'
import AIAnalysisCard from '@/components/interview-center/AIAnalysisCard.vue'
import ConfirmModal from '@/components/common/ConfirmModal.vue'
// 工作流 composables
import { useInterviewPreparation } from '@/composables/useInterviewPreparation'
import { useReviewAnalysis } from '@/composables/useReviewAnalysis'
import { useToast } from '@/composables/useToast'
import { useConfirm } from '@/composables/useConfirm'
import { useAIChat } from '@/composables/useAIChat'
import { useMicrophonePermission } from '@/composables/useMicrophonePermission'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const notificationStore = useNotificationStore()
const { confirm, visible: confirmVisible, title: confirmTitle, message: confirmMessage, confirmText, danger: confirmDanger, handleConfirm, handleCancel } = useConfirm()
const loading = ref(true)
const interview = ref<InterviewDetail | null>(null)
const showAddPreparationDialog = ref(false)
const showReviewDialog = ref(false)
const showEditDialog = ref(false)
// 临时存储 AI 建议参考内容（用于填充到复盘笔记表单）
const referencedAdvice = ref<{
  highPoints: string
  weakPoints: string
  lessonsLearned: string
  overallFeeling: string
} | null>(null)
const showPrepModal = ref(false)
const showMockConfigDialog = ref(false)

// 麦克风权限相关状态
const showPermissionDialog = ref(false)
const permissionDialogState = ref<'checking' | 'requesting' | 'success' | 'denied' | 'permanently_denied' | 'device_error'>('checking')
const permissionErrorMessage = ref<string>('')
const pendingMockConfig = ref<{
  totalQuestions: number
  assistLimit: number
  voiceMode: string
  interviewerStyle: string
} | null>(null)

// 工作流状态
const { state: preparationState, startPreparation } = useInterviewPreparation()
const { state: reviewState, startAnalysis } = useReviewAnalysis()

// 麦克风权限管理
const { checkPermission, requestPermission, releaseStream } = useMicrophonePermission()

// 面试过程文本输入
const sessionTranscript = ref('')

// 职位详情折叠状态
const showPositionDetail = ref(false)

// 优先级排序权重
const PRIORITY_ORDER: Record<string, number> = { required: 0, recommended: 1, optional: 2 }

// 类型排序顺序
const TYPE_ORDER = ['company_research', 'jd_keywords', 'tech_prep', 'behavioral', 'case_study', 'todo', 'manual']

// 按类型分组的准备事项
const groupedPreparations = computed(() => {
  if (!interview.value?.preparations) return {}
  const groups: Record<string, PreparationVO[]> = {}
  for (const prep of interview.value.preparations) {
    const type = prep.itemType || 'manual'
    if (!groups[type]) {
      groups[type] = []
    }
    groups[type].push(prep)
  }
  // 每组内按优先级排序
  for (const type in groups) {
    groups[type].sort((a, b) => {
      const orderA = PRIORITY_ORDER[a.priority || 'recommended'] ?? 1
      const orderB = PRIORITY_ORDER[b.priority || 'recommended'] ?? 1
      return orderA - orderB
    })
  }
  return groups
})

// 分组排序（按类型顺序）
const sortedGroupTypes = computed(() => {
  const types = Object.keys(groupedPreparations.value)
  return types.sort((a, b) => {
    const orderA = TYPE_ORDER.indexOf(a)
    const orderB = TYPE_ORDER.indexOf(b)
    if (orderA === -1 && orderB === -1) return 0
    if (orderA === -1) return 1
    if (orderB === -1) return -1
    return orderA - orderB
  })
})

const statusOptions = INTERVIEW_STATUS_LABELS
const resultOptions = INTERVIEW_RESULT_LABELS

// 是否可以开始模拟面试（必须关联职位）
const canStartMockInterview = computed(() => {
  return !!interview.value?.jobPositionId
})

// 模拟面试按钮提示
const mockInterviewHint = computed(() => {
  if (canStartMockInterview.value) {
    return '基于该面试的职位 JD 进行模拟面试'
  }
  return '请先关联职位后再开始模拟面试'
})

// 是否可以进行 AI 复盘分析（必须有转译文本）
const canStartAIAnalysis = computed(() => {
  return !!sessionTranscript.value?.trim()
})

// AI 分析按钮提示
const aiAnalysisHint = computed(() => {
  if (canStartAIAnalysis.value) {
    return '基于面试过程文本进行 AI 分析'
  }
  return '请先输入或上传面试过程内容'
})

// AI 分析入口区域提示
const aiAnalysisEntryHint = computed(() => {
  if (reviewState.isCompleted) {
    return '分析完成，查看下方结果'
  }
  if (reviewState.isRunning) {
    return '正在分析中...'
  }
  if (canStartAIAnalysis.value) {
    return '已准备好，点击开始分析'
  }
  return '需要先输入面试过程内容'
})

function goBack() {
  router.back()
}

function getRoundTypeLabel(type: string): string {
  return ROUND_TYPE_LABELS[type as keyof typeof ROUND_TYPE_LABELS] || type
}

// 获取轮次标签（支持自定义轮次名称）
function getRoundLabel(interviewData: InterviewDetail): string {
  // 如果是自定义轮次且有自定义名称，显示自定义名称
  if (interviewData.roundType === 'custom' && interviewData.roundName) {
    return interviewData.roundName
  }
  // 否则使用默认标签
  return getRoundTypeLabel(interviewData.roundType!)
}

function getInterviewTypeLabel(type: InterviewType): string {
  return INTERVIEW_TYPE_LABELS[type] || type
}

function formatDateTime(dateStr: string): string {
  return new Date(dateStr).toLocaleString('zh-CN')
}

function copyToClipboard(text: string) {
  navigator.clipboard.writeText(text).then(() => {
    toast.success('已复制到剪贴板')
  }).catch(() => {
    toast.error('复制失败，请手动复制')
  })
}

async function togglePreparation(preparationId: string) {
  if (!interview.value) return
  try {
    await togglePreparationComplete(interview.value.id, preparationId)
    // 直接更新本地状态，避免重新加载整个页面导致滚动位置丢失
    const prep = interview.value.preparations?.find((p) => p.id === preparationId)
    if (prep) {
      prep.completed = !prep.completed
    }
  } catch (error) {
    console.error('切换准备事项状态失败:', error)
  }
}

async function handleDeletePreparation(preparationId: string) {
  if (!interview.value) return
  // 查找准备事项标题
  const prep = interview.value.preparations?.find((p) => p.id === preparationId)
  const prepTitle = prep?.title || '该准备事项'
  // 二次确认
  const confirmed = await confirm({
    title: '删除准备事项',
    message: `确定要删除准备事项「${prepTitle}」吗？此操作不可撤销。`,
    danger: true
  })
  if (!confirmed) return
  try {
    await deletePreparationApi(interview.value.id, preparationId)
    // 直接从本地数组移除，避免重新加载整个页面导致刷新
    if (interview.value.preparations) {
      interview.value.preparations = interview.value.preparations.filter(
        (p) => p.id !== preparationId
      )
    }
  } catch (error) {
    console.error('删除准备事项失败:', error)
  }
}

async function handleDelete() {
  if (!interview.value) return
  const confirmed = await confirm({
    title: '删除确认',
    message: `确定要删除面试「${interview.value.companyName} - ${interview.value.position}」吗？此操作不可撤销。`,
    danger: true
  })
  if (!confirmed) return
  try {
    await deleteInterview(interview.value.id)
    router.push('/interview-center')
  } catch (error) {
    console.error('删除面试失败:', error)
  }
}

async function handleStatusChange(newStatus: string) {
  if (!interview.value || interview.value.status === newStatus) return
  try {
    await updateInterview(interview.value.id, { status: newStatus as InterviewStatus })
    interview.value.status = newStatus as InterviewStatus
  } catch (error) {
    console.error('更新面试状态失败:', error)
    toast.error('更新状态失败，请稍后重试')
  }
}

async function handleResultChange(newResult: string) {
  if (!interview.value) return
  try {
    const result = newResult ? (newResult as InterviewResult) : undefined
    await updateInterview(interview.value.id, { overallResult: result })
    interview.value.overallResult = result
  } catch (error) {
    console.error('更新面试结果失败:', error)
    toast.error('更新结果失败，请稍后重试')
  }
}

async function loadDetail() {
  const id = route.params.id as string
  if (!id) return
  loading.value = true
  try {
    interview.value = await getInterviewDetail(id)
    // 加载时读取 transcript 字段
    if (interview.value?.transcript) {
      sessionTranscript.value = interview.value.transcript
    }
  } catch (error) {
    console.error('加载面试详情失败:', error)
  } finally {
    loading.value = false
  }
}

// ===== AI 工作流处理 =====

function handleAIGeneratePreparation() {
  if (!interview.value) return
  showPrepModal.value = true
  startPreparation(interview.value.id)
}

// 弹窗保存事件
async function handleSavePreparationItems(items: typeof preparationState.preparationItems) {
  if (!interview.value || items.length === 0) return

  try {
    // 批量保存，传递完整字段
    // contentItems 数组转成 JSON 字符串存入 content
    await batchAddPreparations(interview.value!.id, items.map(item => ({
      title: item.title,
      content: item.contentItems && item.contentItems.length > 0
        ? JSON.stringify(item.contentItems)
        : (item.content || ''),
      itemType: item.itemType,
      priority: item.priority
    })))
    showPrepModal.value = false
    loadDetail()
  } catch (error) {
    console.error('保存准备事项失败:', error)
    toast.error('保存失败，请稍后重试')
  }
}

// 弹窗取消事件
function handleCancelPreparation() {
  showPrepModal.value = false
}

// 弹窗重试事件
function handleRetryPreparation() {
  if (!interview.value) return
  startPreparation(interview.value.id)
}

function handleAIAnalysis() {
  if (!interview.value || !canStartAIAnalysis.value) return
  startAnalysis(interview.value.id, sessionTranscript.value)
}

// 重新分析
async function handleReanalyze() {
  if (!interview.value) return

  // 弹出确认框
  const confirmed = await confirm({
    title: '重新分析',
    message: '重新分析将覆盖之前的 AI 分析结果，确定继续吗？',
    confirmText: '确定',
    danger: true
  })

  if (confirmed) {
    // 重置状态
    reviewState.isCompleted = false
    reviewState.adviceList = []
    reviewState.hasError = false
    // 重新分析
    startAnalysis(interview.value.id, sessionTranscript.value)
  }
}

// 参考 AI 建议
function handleReferenceAIAdvice(adviceList: AdviceItem[]) {
  if (!interview.value || adviceList.length === 0) return

  // 将 AI 建议分类整理
  const highPoints: string[] = []
  const weakPoints: string[] = []
  const lessonsLearned: string[] = []

  adviceList.forEach(advice => {
    const category = advice.category?.toLowerCase() || ''
    let text = `${advice.title}: ${advice.description}`
    if (advice.actionItems && advice.actionItems.length > 0) {
      text += '\n' + advice.actionItems.map(item => `  - ${item}`).join('\n')
    }

    // 根据类别分类
    if (category.includes('技能') || category.includes('知识')) {
      lessonsLearned.push(text)
    } else if (category.includes('表现') || category.includes('优点') || category.includes('亮点')) {
      highPoints.push(text)
    } else if (category.includes('不足') || category.includes('改进') || category.includes('问题')) {
      weakPoints.push(text)
    } else {
      lessonsLearned.push(text)
    }
  })

  // 弹出确认框
  confirm({
    title: '参考 AI 建议',
    message: '是否将 AI 分析建议填充到复盘笔记？这将覆盖原有内容（如有）',
    confirmText: '填充'
  }).then(confirmed => {
    if (confirmed && interview.value) {
      // 临时存储参考内容，用于 ReviewNoteDialog 加载
      referencedAdvice.value = {
        highPoints: highPoints.join('\n\n') || interview.value.reviewNote?.highPoints || '',
        weakPoints: weakPoints.join('\n\n') || interview.value.reviewNote?.weakPoints || '',
        lessonsLearned: lessonsLearned.join('\n\n') || interview.value.reviewNote?.lessonsLearned || '',
        overallFeeling: interview.value.reviewNote?.overallFeeling || ''
      }
      // 打开复盘笔记编辑弹窗
      showReviewDialog.value = true
    }
  })
}

// 保存转译文本
async function handleSaveTranscript(transcript: string) {
  if (!interview.value) return
  try {
    await updateInterview(interview.value.id, { transcript })
    sessionTranscript.value = transcript
  } catch (error) {
    console.error('保存转译文本失败:', error)
    toast.error('保存失败，请稍后重试')
  }
}

// 开始模拟面试 - 打开配置弹窗
function startMockInterview() {
  if (!interview.value || !canStartMockInterview.value) {
    toast.error(mockInterviewHint.value)
    return
  }
  showMockConfigDialog.value = true
}

// 处理模拟面试配置提交
async function handleMockConfigSubmit(config: {
  totalQuestions: number
  assistLimit: number
  voiceMode: string
  interviewerStyle: string
}) {
  if (!interview.value) return

  showMockConfigDialog.value = false
  pendingMockConfig.value = config

  // 先静默检查权限，不显示弹窗
  try {
    const checkResult = await checkPermission()

    if (checkResult.state === 'granted') {
      // 已授权，直接进入模拟面试，不显示任何弹窗
      createSessionAndNavigate()
      return
    }

    // 未授权或需要请求权限，显示弹窗
    if (checkResult.state === 'denied') {
      permissionDialogState.value = 'denied'
    } else {
      permissionDialogState.value = 'requesting'
    }
    showPermissionDialog.value = true
    await requestMicrophonePermission()
  } catch (e) {
    console.error('[InterviewDetail] 检查麦克风权限失败:', e)
    permissionDialogState.value = 'device_error'
    permissionErrorMessage.value = '检查权限时发生错误'
    showPermissionDialog.value = true
  }
}

// 请求麦克风权限
async function requestMicrophonePermission() {
  try {
    const result = await requestPermission()

    if (result.state === 'granted') {
      // 权限获取成功，释放临时的 stream
      if (result.stream) {
        releaseStream(result.stream)
      }
      permissionDialogState.value = 'success'
      setTimeout(() => {
        showPermissionDialog.value = false
        createSessionAndNavigate()
      }, 500)
    } else if (result.isPermanentlyDenied) {
      permissionDialogState.value = 'permanently_denied'
    } else {
      permissionDialogState.value = 'denied'
    }
  } catch (e) {
    console.error('[InterviewDetail] 请求麦克风权限失败:', e)
    permissionDialogState.value = 'device_error'
    permissionErrorMessage.value = e instanceof Error ? e.message : '请求权限失败'
  }
}

// 权限弹窗重试
async function handlePermissionRetry() {
  permissionDialogState.value = 'requesting'
  await requestMicrophonePermission()
}

// 权限弹窗关闭
function handlePermissionDialogClose() {
  showPermissionDialog.value = false
  pendingMockConfig.value = null
}

// 创建会话并导航
async function createSessionAndNavigate() {
  if (!interview.value || !pendingMockConfig.value) return

  const config = pendingMockConfig.value
  pendingMockConfig.value = null

  try {
    const response = await createSession({
      interviewId: interview.value.id,
      totalQuestions: config.totalQuestions,
      assistLimit: config.assistLimit,
      voiceMode: config.voiceMode,
      interviewerStyle: config.interviewerStyle
    })
    router.push(`/interview-center/${interview.value.id}/mock/${response.sessionId}`)
  } catch (error) {
    console.error('创建模拟面试失败:', error)
    toast.error('创建模拟面试失败，请稍后重试')
  }
}

// 弹窗事件处理
function handlePreparationAdded() {
  loadDetail()
}

function handleReviewSaved() {
  loadDetail()
}

function handleInterviewUpdated() {
  loadDetail()
}

// AI 聊天状态
const { state: aiChatState } = useAIChat()

// 统一处理弹窗隐藏悬浮球
function updateFloatVisibility(show: boolean) {
  aiChatState.hideFloat = show
}

// 监听弹窗状态
watch(showPrepModal, updateFloatVisibility)
watch(showMockConfigDialog, updateFloatVisibility)
watch(showPermissionDialog, updateFloatVisibility)

onMounted(() => {
  loadDetail()
  // 监听转录完成事件（从 AppNavbar 触发）
  window.addEventListener('apply-transcript', handleApplyTranscriptEvent as EventListener)
})

onUnmounted(() => {
  window.removeEventListener('apply-transcript', handleApplyTranscriptEvent as EventListener)
})

// 处理转录完成事件
function handleApplyTranscriptEvent(event: Event) {
  const customEvent = event as CustomEvent
  const { task } = customEvent.detail
  if (task?.taskType === 'audio_transcribe' && task?.businessId === interview.value?.id) {
    try {
      const result = JSON.parse(task.result)
      sessionTranscript.value = result.transcriptText
      toast.success('转录完成，已自动保存到面试记录')
      // 标记已应用
      notificationStore.markTranscribeApplied(interview.value!.id)
    } catch (e) {
      console.error('[InterviewDetail] 解析转录结果失败:', e)
    }
  }
}
</script>

<style scoped lang="scss">
.interview-detail-page {
  padding: $spacing-xl;
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-xl;
}

.header-left {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: transparent;
  border: 1px solid $color-bg-elevated;
  color: $color-text-secondary;
  cursor: pointer;
  border-radius: $radius-md;
  transition: all 0.2s;

  svg {
    transition: transform 0.2s;
  }

  &:hover {
    color: $color-text-primary;
    background: $color-bg-secondary;
    border-color: rgba(255, 255, 255, 0.1);

    svg {
      transform: translateX(-2px);
    }
  }
}

.header-title {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  font-size: 1.125rem;

  .company-name {
    font-weight: 600;
    color: $color-text-primary;
  }

  .separator {
    color: $color-text-tertiary;
  }

  .position-name {
    color: $color-text-secondary;
  }

  .round-badge {
    font-size: 0.75rem;
    padding: 4px 10px;
    background: rgba($color-accent, 0.15);
    color: $color-accent;
    border-radius: $radius-full;
  }
}

.header-actions {
  display: flex;
  gap: $spacing-sm;
}

// 面试信息卡片
.interview-info-card {
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  margin-bottom: $spacing-xl;
}

.info-row {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-lg;
  margin-bottom: $spacing-md;

  &:last-child {
    margin-bottom: 0;
  }
}

.info-item {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: 0.875rem;

  .info-icon {
    font-size: 1rem;
  }

  .info-label {
    color: $color-text-tertiary;
  }

  .info-value {
    color: $color-text-primary;
  }

  .link-value {
    color: $color-accent;
    text-decoration: none;
    word-break: break-all;

    &:hover {
      text-decoration: underline;
    }
  }

  .resume-link {
    color: $color-accent;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }

  .notes-item {
    align-items: flex-start;
  }

  .notes-content {
    white-space: pre-wrap;
    word-break: break-word;
  }

  .copy-btn {
    padding: 2px 8px;
    font-size: 0.75rem;
    background: transparent;
    border: 1px solid $color-bg-elevated;
    border-radius: $radius-sm;
    color: $color-text-tertiary;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      border-color: $color-accent;
      color: $color-accent;
    }
  }
}

.status-select {
  appearance: none;
  background: transparent;
  border: none;
  font-size: 0.875rem;
  padding: 4px 24px 4px 8px;
  border-radius: $radius-sm;
  cursor: pointer;
  color: inherit;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='10' viewBox='0 0 10 10'%3E%3Cpath fill='%23a1a1aa' d='M5 7L1 3h8z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
  transition: background-color 0.2s;

  &:hover {
    background-color: rgba($color-text-tertiary, 0.1);
  }

  &:focus {
    outline: none;
  }

  &.preparing {
    color: $color-warning;
  }

  &.in_progress {
    color: $color-info;
  }

  &.completed {
    color: $color-success;
  }

  &.cancelled {
    color: $color-text-tertiary;
  }

  option {
    background: $color-bg-secondary;
    color: $color-text-primary;
  }
}

.result-select {
  appearance: none;
  background: transparent;
  border: 1px solid $color-bg-tertiary;
  font-size: 0.875rem;
  padding: 4px 28px 4px 8px;
  border-radius: $radius-sm;
  cursor: pointer;
  color: $color-text-secondary;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='10' viewBox='0 0 10 10'%3E%3Cpath fill='%23a1a1aa' d='M5 7L1 3h8z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
  transition: border-color 0.2s;

  &:hover {
    border-color: $color-accent;
  }

  &:focus {
    outline: none;
    border-color: $color-accent;
  }

  option {
    background: $color-bg-secondary;
    color: $color-text-primary;
  }
}

// 职位详情折叠
.position-detail-toggle {
  margin-top: $spacing-lg;
  padding-top: $spacing-md;
  border-top: 1px solid $color-bg-elevated;
}

.toggle-btn {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  background: transparent;
  border: none;
  color: $color-text-secondary;
  cursor: pointer;
  font-size: 0.875rem;
  padding: $spacing-sm;
  border-radius: $radius-md;
  transition: all 0.2s;

  &:hover {
    color: $color-accent;
    background: rgba($color-accent, 0.1);
  }

  svg {
    width: 16px;
    height: 16px;
    transition: transform 0.2s;

    &.rotated {
      transform: rotate(180deg);
    }
  }
}

.position-detail-content {
  margin-top: $spacing-md;
  padding: $spacing-md;
  background: $color-bg-tertiary;
  border-radius: $radius-md;

  .detail-item {
    margin-bottom: $spacing-lg;

    &:last-child {
      margin-bottom: 0;
    }

    h4 {
      font-size: 0.875rem;
      font-weight: 600;
      color: $color-text-secondary;
      margin-bottom: $spacing-sm;
    }

    .detail-text {
      color: $color-text-primary;
      font-size: 0.875rem;
      line-height: 1.6;
      white-space: pre-wrap;
    }
  }
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-lg;

  h2 {
    font-size: 1.25rem;
    font-weight: 600;
    color: $color-text-primary;
  }

  .header-actions {
    display: flex;
    gap: $spacing-sm;
  }
}

.btn-danger {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: $color-text-tertiary;
  padding: $spacing-xs $spacing-sm;
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  transition: all 0.2s;

  &:hover {
    color: $color-error;
    background: rgba($color-error, 0.15);
    border-color: $color-error;
  }
}

.btn-ai {
  background: linear-gradient(135deg, $color-accent 0%, $color-accent-dark 100%);
  color: $color-bg-primary;
  border: none;
  font-weight: 500;

  &:hover:not(:disabled) {
    opacity: 0.9;
    transform: translateY(-1px);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.btn-mock-interview {
  background: linear-gradient(135deg, #4ade80 0%, #22c55e 100%);
  color: $color-bg-primary;
  border: none;
  font-weight: 500;
  padding: $spacing-sm $spacing-md;

  &:hover:not(:disabled) {
    opacity: 0.9;
    transform: translateY(-1px);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    background: $color-bg-tertiary;
    color: $color-text-tertiary;
  }
}

.progress-indicator {
  margin-bottom: $spacing-lg;
  padding: $spacing-md;
  background: $color-bg-tertiary;
  border-radius: $radius-md;

  .progress-bar {
    height: 4px;
    background: $color-bg-elevated;
    border-radius: 2px;
    overflow: hidden;
    margin-bottom: $spacing-sm;
  }

  .progress-fill {
    height: 100%;
    background: linear-gradient(90deg, $color-accent, $color-accent-light);
    transition: width 0.3s ease;
  }

  .progress-message {
    font-size: 0.875rem;
    color: $color-text-secondary;
    margin: 0;
  }
}

.ai-analysis-result {
  margin-bottom: $spacing-lg;
  padding: $spacing-lg;
  background: rgba($color-info, 0.1);
  border: 1px solid rgba($color-info, 0.2);
  border-radius: $radius-md;

  h4 {
    font-size: 1rem;
    font-weight: 600;
    color: $color-info;
    margin-bottom: $spacing-md;
  }

  .advice-list {
    display: flex;
    flex-direction: column;
    gap: $spacing-md;
  }

  .advice-item {
    padding: $spacing-sm;
    background: $color-bg-secondary;
    border-radius: $radius-sm;

    .advice-header {
      display: flex;
      align-items: center;
      gap: $spacing-sm;
      margin-bottom: $spacing-xs;
    }

    .advice-category {
      font-size: 0.625rem;
      padding: 2px 6px;
      background: $color-bg-tertiary;
      border-radius: $radius-sm;
      color: $color-text-tertiary;
    }

    .advice-title {
      font-size: 0.875rem;
      font-weight: 500;
      color: $color-text-primary;
    }

    .advice-description {
      font-size: 0.8125rem;
      color: $color-text-secondary;
      line-height: 1.5;
      margin: 0;
    }
  }
}

.preparations-section, .review-section {
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  padding: $spacing-xl;
  margin-bottom: $spacing-xl;
}

.preparations-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.preparation-item {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  padding: $spacing-md;
  background: $color-bg-tertiary;
  border-radius: $radius-md;

  &.completed {
    opacity: 0.6;

    .prep-title {
      text-decoration: line-through;
    }
  }

  input[type="checkbox"] {
    width: 18px;
    height: 18px;
    accent-color: $color-accent;
  }

  .prep-title {
    flex: 1;
    color: $color-text-primary;
  }

  .delete-btn {
    background: none;
    border: none;
    color: $color-text-tertiary;
    cursor: pointer;
    font-size: 1.25rem;

    &:hover {
      color: $color-error;
    }
  }
}

.review-note {
  .note-header {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    margin-bottom: $spacing-md;
    padding-bottom: $spacing-sm;
    border-bottom: 1px solid $color-bg-elevated;
  }

  .note-icon {
    font-size: 1rem;
  }

  .note-title{
    font-size: 0.9375rem;
    font-weight: 600;
    color: $color-text-primary;
    flex: 1;
  }

  .btn-edit-note {
    background: transparent;
    border: 1px solid $color-bg-elevated;
    color: $color-text-tertiary;
    padding: 4px 10px;
    font-size: 0.75rem;
    border-radius: $radius-sm;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      color: $color-accent;
      border-color: $color-accent;
    }
  }

  .note-section {
    margin-bottom: $spacing-md;

    &:last-child {
      margin-bottom: 0;
    }

    h4 {
      font-size: 0.8125rem;
      font-weight: 600;
      color: $color-text-tertiary;
      margin-bottom: $spacing-xs;
    }

    p {
      color: $color-text-primary;
      line-height: 1.6;
      font-size: 0.875rem;
      white-space: pre-wrap;
      word-break: break-word;
    }
  }
}

// AI 分析入口区域
.ai-analysis-entry {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: $spacing-md;
  padding: $spacing-md;
  background: $color-bg-tertiary;
  border-radius: $radius-md;

  .entry-left {
    display: flex;
    flex-direction: column;
    gap: $spacing-xs;
  }

  .entry-label {
    font-size: 0.9375rem;
    font-weight: 600;
    color: $color-text-primary;
  }

  .entry-hint {
    font-size: 0.75rem;
    color: $color-text-tertiary;
  }

  .entry-right {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
  }

  .ai-btn-wrapper {
    position: relative;
  }

  .btn-ai {
    background: linear-gradient(135deg, $color-accent 0%, $color-accent-dark 100%);
    color: $color-bg-primary;
    border: none;
    font-weight: 500;

    &:hover:not(:disabled) {
      opacity: 0.9;
      transform: translateY(-1px);
    }

    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  }

  .btn-manual {
    background: transparent;
    border: 1px solid $color-bg-elevated;
    color: $color-text-secondary;

    &:hover {
      border-color: $color-accent;
      color: $color-accent;
    }
  }

  // Tooltip（用于禁用按钮的 hover 提示）
  .tooltip-wrapper {
    position: relative;

    .tooltip-text {
      visibility: hidden;
      opacity: 0;
      position: absolute;
      bottom: calc(100% + 8px);
      left: 50%;
      transform: translateX(-50%);
      background: $color-bg-elevated;
      color: $color-text-primary;
      padding: $spacing-xs $spacing-sm;
      border-radius: $radius-sm;
      font-size: 0.75rem;
      white-space: nowrap;
      z-index: 100;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
      border: 1px solid rgba(255, 255, 255, 0.1);
      transition: opacity 0.2s, visibility 0.2s;

      // 小箭头
      &::after {
        content: '';
        position: absolute;
        top: 100%;
        left: 50%;
        transform: translateX(-50%);
        border: 6px solid transparent;
        border-top-color: $color-bg-elevated;
      }
    }

    &:hover .tooltip-text {
      visibility: visible;
      opacity: 1;
    }
  }
}

.section-divider {
  height: 1px;
  background: $color-bg-elevated;
  margin: $spacing-lg 0;
}

.empty-preparations, .empty-review {
  text-align: center;
  padding: $spacing-xl;
  color: $color-text-tertiary;
}

.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  color: $color-text-tertiary;
}
</style>
