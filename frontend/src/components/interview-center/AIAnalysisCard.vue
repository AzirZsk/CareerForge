<template>
  <div class="ai-analysis-card" :class="{ expanded: isExpanded }">
    <!-- 卡片头部 -->
    <div class="card-header" @click="toggleExpand">
      <div class="header-left">
        <font-awesome-icon icon="fa-solid fa-robot" class="card-icon" />
        <span class="card-title">AI 分析建议</span>
        <span v-if="generatedTime" class="generated-time">
          生成于 {{ formatTime(generatedTime) }}
        </span>
      </div>
      <div class="header-right">
        <button
          v-if="!hideActions"
          class="btn btn-sm btn-reanalyze"
          @click.stop="handleReanalyze"
          :disabled="isAnalyzing"
        >
          {{ isAnalyzing ? '分析中...' : '重新分析' }}
        </button>
        <font-awesome-icon
          icon="fa-solid fa-chevron-down"
          class="expand-icon"
          :class="{ rotated: isExpanded }"
        />
      </div>
    </div>

    <!-- 卡片内容（可折叠） -->
    <div v-if="isExpanded" class="card-content">
      <!-- 面试表现分析（来自持久化结果） -->
      <div v-if="interviewAnalysis" class="analysis-section">
        <div class="section-header-inline" @click="toggleInterviewAnalysis">
          <span class="section-title">面试表现分析</span>
          <font-awesome-icon icon="fa-solid fa-chevron-down" :class="{ rotated: showInterviewAnalysis }" />
        </div>
        <Transition name="expand">
          <div v-if="showInterviewAnalysis" class="section-content">
            <InterviewAnalysisContent :data="interviewAnalysis" />
          </div>
        </Transition>
      </div>

      <!-- 对话分析详情（来自持久化结果） -->
      <div v-if="transcriptAnalysis" class="analysis-section">
        <div class="section-header-inline" @click="toggleTranscriptAnalysis">
          <span class="section-title">对话分析详情</span>
          <font-awesome-icon icon="fa-solid fa-chevron-down" :class="{ rotated: showTranscriptAnalysis }" />
        </div>
        <Transition name="expand">
          <div v-if="showTranscriptAnalysis" class="section-content">
            <TranscriptAnalysisContent :data="transcriptAnalysis" />
          </div>
        </Transition>
      </div>

      <!-- AI 建议列表 -->
      <div v-if="adviceList.length > 0" class="advice-list">
        <div v-for="(advice, index) in adviceList" :key="index" class="advice-item">
          <div class="advice-header">
            <span v-if="advice.category" class="advice-category">{{ advice.category }}</span>
            <span class="advice-title">{{ advice.title }}</span>
            <span v-if="advice.priority" class="advice-priority" :class="advice.priority">
              {{ getPriorityLabel(advice.priority) }}
            </span>
          </div>
          <p class="advice-description">{{ advice.description }}</p>
          <ul v-if="advice.actionItems && advice.actionItems.length > 0" class="action-items">
            <li v-for="(item, idx) in advice.actionItems" :key="idx">{{ item }}</li>
          </ul>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <p>暂无 AI 分析结果</p>
      </div>

      <!-- 参考建议按钮 -->
      <div v-if="showReferenceButton && adviceList.length > 0" class="card-footer">
        <button class="btn btn-sm btn-reference" @click="handleReference">
          <font-awesome-icon icon="fa-solid fa-clipboard" /> 参考 AI 建议写笔记
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import TranscriptAnalysisContent from './review/TranscriptAnalysisContent.vue'
import InterviewAnalysisContent from './review/InterviewAnalysisContent.vue'
import type { AIAnalysisVO, AdviceItem, TranscriptAnalysisResult, InterviewAnalysisResult } from '@/types/interview-center'

const props = defineProps<{
  /** AI 分析记录（包含 adviceList 字段） */
  aiAnalysisNote?: AIAnalysisVO | null
  /** 是否正在分析中 */
  isAnalyzing?: boolean
  /** 是否显示操作按钮（重新分析、参考建议） */
  hideActions?: boolean
  /** 是否显示参考建议按钮 */
  showReferenceButton?: boolean
  /** 是否默认展开 */
  defaultExpanded?: boolean
}>()

const emit = defineEmits<{
  (e: 'reanalyze'): void
  (e: 'reference', adviceList: AdviceItem[]): void
}>()

const isExpanded = ref(props.defaultExpanded ?? false)

// AI 建议列表（直接从后端 VO 的 adviceList 字段读取）
const adviceList = computed<AdviceItem[]>(() => {
  return props.aiAnalysisNote?.adviceList ?? []
})

// 生成时间
const generatedTime = computed(() => {
  return props.aiAnalysisNote?.createdAt
})

// 对话分析结果
const transcriptAnalysis = computed<TranscriptAnalysisResult | undefined>(() => {
  return props.aiAnalysisNote?.transcriptAnalysis
})

// 面试分析结果
const interviewAnalysis = computed<InterviewAnalysisResult | undefined>(() => {
  return props.aiAnalysisNote?.interviewAnalysis
})

// 是否有扩展分析数据
const hasExtendedAnalysis = computed(() => {
  return transcriptAnalysis.value || interviewAnalysis.value
})

// 折叠状态
const showInterviewAnalysis = ref(false)
const showTranscriptAnalysis = ref(false)

// 格式化时间
function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取优先级标签
function getPriorityLabel(priority: string): string {
  const labels: Record<string, string> = {
    high: '高',
    medium: '中',
    low: '低'
  }
  return labels[priority] || priority
}

// 切换展开/折叠
function toggleExpand() {
  isExpanded.value = !isExpanded.value
}

// 切换面试分析展开/折叠
function toggleInterviewAnalysis() {
  showInterviewAnalysis.value = !showInterviewAnalysis.value
}

// 切换对话分析展开/折叠
function toggleTranscriptAnalysis() {
  showTranscriptAnalysis.value = !showTranscriptAnalysis.value
}

// 重新分析
function handleReanalyze() {
  emit('reanalyze')
}

// 参考建议
function handleReference() {
  emit('reference', adviceList.value)
}

// 监听分析状态，完成后自动展开
watch(() => props.isAnalyzing, (newValue, oldValue) => {
  if (oldValue === true && newValue === false) {
    // 分析完成，自动展开
    isExpanded.value = true
  }
})

// 初始化时如果有数据且默认展开，则展开
onMounted(() => {
  if (props.defaultExpanded && adviceList.value.length > 0) {
    isExpanded.value = true
  }
})
</script>

<style scoped lang="scss">
.ai-analysis-card {
  background: $color-bg-secondary;
  border: 1px solid $color-border;
  border-radius: $radius-md;
  overflow: hidden;
  transition: all 0.2s;

  &.expanded {
    background: $color-bg-tertiary;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-md;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: $color-bg-tertiary;
  }
}

.header-left {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.card-icon {
  font-size: 1.125rem;
  color: $color-accent;
}

.card-title {
  font-size: 0.9375rem;
  font-weight: 600;
  color: $color-accent;
}

.generated-time {
  font-size: 0.75rem;
  color: $color-text-tertiary;
  margin-left: $spacing-sm;
}

.header-right {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.btn-reanalyze {
  background: transparent;
  border: 1px solid rgba($color-accent, 0.3);
  color: $color-accent;
  padding: 4px 10px;
  font-size: 0.75rem;
  border-radius: $radius-sm;
  cursor: pointer;
  transition: all 0.2s;

  &:hover:not(:disabled) {
    background: rgba($color-accent, 0.15);
    border-color: $color-accent;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.expand-icon {
  color: $color-text-tertiary;
  transition: transform 0.2s;

  &.rotated {
    transform: rotate(180deg);
  }
}

.card-content {
  padding: 0 $spacing-md $spacing-md;
  border-top: 1px solid $color-border;
}

.analysis-section {
  margin-top: $spacing-md;

  &:first-child {
    margin-top: $spacing-sm;
  }
}

.section-header-inline {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-sm;
  background: $color-bg-secondary;
  border-radius: $radius-sm;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s;

  &:hover {
    background: $color-bg-elevated;
  }
}

.section-title {
  font-size: 0.875rem;
  font-weight: 500;
  color: $color-text-primary;
}

.section-content {
  padding: $spacing-sm;
  margin-top: $spacing-xs;
}

// 展开/折叠动画
.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  max-height: 0;
  opacity: 0;
}

.expand-enter-to,
.expand-leave-from {
  max-height: 1000px;
  opacity: 1;
}

.advice-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
  margin-top: $spacing-md;
}

.advice-item {
  padding: $spacing-sm;
  background: $color-bg-secondary;
  border-radius: $radius-sm;
}

.advice-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-xs;
  flex-wrap: wrap;
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

.advice-priority {
  font-size: 0.625rem;
  padding: 2px 6px;
  border-radius: $radius-sm;

  &.high {
    background: rgba($color-error, 0.15);
    color: $color-error;
  }

  &.medium {
    background: rgba($color-warning, 0.15);
    color: $color-warning;
  }

  &.low {
    background: rgba($color-success, 0.15);
    color: $color-success;
  }
}

.advice-description {
  font-size: 0.8125rem;
  color: $color-text-secondary;
  line-height: 1.5;
  margin: 0;
}

.action-items {
  margin: $spacing-sm 0 0;
  padding-left: $spacing-lg;
  font-size: 0.8125rem;
  color: $color-text-secondary;

  li {
    margin-bottom: 4px;

    &:last-child {
      margin-bottom: 0;
    }
  }
}

.empty-state {
  text-align: center;
  padding: $spacing-xl;
  color: $color-text-tertiary;
}

.card-footer {
  margin-top: $spacing-md;
  padding-top: $spacing-md;
  border-top: 1px solid $color-border;
}

.btn-reference {
  width: 100%;
  background: rgba($color-accent, 0.15);
  border: 1px solid rgba($color-accent, 0.3);
  color: $color-accent;
  padding: $spacing-sm;
  font-size: 0.875rem;
  border-radius: $radius-sm;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: rgba($color-accent, 0.25);
    border-color: $color-accent;
  }
}
</style>
