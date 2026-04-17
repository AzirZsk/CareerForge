<!--=====================================================
  模拟面试详情弹窗（全屏模态框）
  复用 review 子组件展示 AI 分析结果
  @author Azir
=====================================================-->

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="modal-overlay" @click.self="close">
        <div class="modal-container">
          <!-- 头部 -->
          <header class="modal-header">
            <button class="header-btn back-btn" @click="close">
              <font-awesome-icon icon="fa-solid fa-arrow-left" />
              返回
            </button>
            <h2 class="header-title">模拟面试分析</h2>
            <button class="header-btn close-btn" @click="close">
              <font-awesome-icon icon="fa-solid fa-xmark" />
            </button>
          </header>

          <!-- 加载状态 -->
          <div v-if="loading" class="loading-state">
            <div class="spinner" />
            <p>加载分析数据...</p>
          </div>

          <!-- 内容 -->
          <div v-else-if="detail" class="modal-body">
            <!-- 摘要信息栏 -->
            <div class="summary-bar">
              <div class="summary-score">
                <div class="big-score-ring" :style="{ '--score': detail.summary.overallScore || 0 }">
                  <span class="score-value">{{ detail.summary.overallScore ?? '-' }}</span>
                  <span class="score-label">评分</span>
                </div>
              </div>
              <div class="summary-info">
                <div class="info-row">
                  <span class="info-label">{{ formatDate(detail.summary.createdAt) }}</span>
                  <span class="info-tag">{{ styleLabel(detail.summary.interviewerStyle) }}</span>
                  <span class="info-tag tag-blue">{{ modeLabel(detail.summary.voiceMode) }}</span>
                </div>
                <div class="info-row">
                  <span class="info-item">{{ detail.summary.duration ? `${detail.summary.duration}分钟` : '--' }}</span>
                  <span class="info-item">{{ detail.summary.questions || 0 }} 题</span>
                  <span class="info-item">求助 {{ detail.summary.assistCount ?? 0 }}/{{ detail.summary.assistLimit ?? 0 }}</span>
                  <span v-if="detail.summary.overallPerformance" class="info-item performance" :class="performanceClass">
                    {{ detail.summary.overallPerformance }}
                  </span>
                  <span v-if="detail.summary.jdMatchScore" class="info-item">
                    JD匹配 {{ detail.summary.jdMatchScore }}/10
                  </span>
                </div>
              </div>
            </div>

            <!-- Tab 切换 -->
            <div class="tabs">
              <button
                v-for="tab in tabs"
                :key="tab.key"
                class="tab-btn"
                :class="{ active: activeTab === tab.key }"
                @click="activeTab = tab.key"
              >
                <font-awesome-icon :icon="tab.icon" />
                {{ tab.label }}
              </button>
            </div>

            <!-- Tab 内容 -->
            <div class="tab-content">
              <!-- 对话分析 -->
              <div v-show="activeTab === 'transcript'" class="tab-panel">
                <TranscriptAnalysisContent
                  v-if="detail.aiAnalysis?.transcriptAnalysis"
                  :data="detail.aiAnalysis.transcriptAnalysis"
                />
                <div v-else class="no-data">暂无对话分析数据</div>
              </div>

              <!-- 面试分析 -->
              <div v-show="activeTab === 'interview'" class="tab-panel">
                <InterviewAnalysisContent
                  v-if="detail.aiAnalysis?.interviewAnalysis"
                  :data="detail.aiAnalysis.interviewAnalysis"
                />
                <div v-else class="no-data">暂无面试分析数据</div>
              </div>

              <!-- 改进建议 -->
              <div v-show="activeTab === 'advice'" class="tab-panel">
                <AdviceListContent
                  v-if="detail.aiAnalysis?.adviceList?.length"
                  :data="detail.aiAnalysis.adviceList"
                />
                <div v-else class="no-data">暂无改进建议</div>
              </div>
            </div>
          </div>

          <!-- 错误状态 -->
          <div v-else class="error-state">
            <font-awesome-icon icon="fa-solid fa-circle-xmark" class="error-icon" />
            <p>加载失败，请稍后重试</p>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { getMockSessionDetail } from '@/api/interview-center'
import type { MockSessionDetail } from '@/types/interview-center'
import TranscriptAnalysisContent from './review/TranscriptAnalysisContent.vue'
import InterviewAnalysisContent from './review/InterviewAnalysisContent.vue'
import AdviceListContent from './review/AdviceListContent.vue'

const props = defineProps<{
  visible: boolean
  mockInterviewId: string
  interviewId: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
}>()

const loading = ref(false)
const detail = ref<MockSessionDetail | null>(null)
const activeTab = ref('transcript')

const tabs = [
  { key: 'transcript', label: '对话分析', icon: 'fa-solid fa-comments' },
  { key: 'interview', label: '面试分析', icon: 'fa-solid fa-chart-bar' },
  { key: 'advice', label: '改进建议', icon: 'fa-solid fa-lightbulb' }
]

const performanceClass = computed(() => {
  const p = detail.value?.summary?.overallPerformance
  if (p === '优秀') return 'perf-excellent'
  if (p === '良好') return 'perf-good'
  if (p === '一般') return 'perf-average'
  return 'perf-poor'
})

watch(() => props.visible, async (val) => {
  if (val && props.mockInterviewId) {
    loading.value = true
    activeTab.value = 'transcript'
    try {
      detail.value = await getMockSessionDetail(props.interviewId, props.mockInterviewId)
    } catch (e) {
      console.error('加载模拟面试详情失败:', e)
      detail.value = null
    } finally {
      loading.value = false
    }
  }
})

function close(): void {
  emit('update:visible', false)
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '--'
  const d = new Date(dateStr)
  return `${d.getMonth() + 1}月${d.getDate()}日 ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

function styleLabel(style: string): string {
  const map: Record<string, string> = { professional: '专业严肃', friendly: '亲和引导', challenging: '压力挑战' }
  return map[style] || style || '--'
}

function modeLabel(mode: string): string {
  const map: Record<string, string> = { half_voice: '半语音', full_voice: '全语音', text: '文字' }
  return map[mode] || mode || '--'
}
</script>

<style lang="scss" scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.8);
  z-index: $z-modal;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-container {
  width: 90vw;
  max-width: 900px;
  height: 85vh;
  background: $color-bg-primary;
  border-radius: $radius-xl;
  border: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

// 头部
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-md $spacing-lg;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  flex-shrink: 0;
}

.header-btn {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-xs $spacing-sm;
  font-size: $text-sm;
  color: $color-text-secondary;
  border-radius: $radius-sm;
  transition: all $transition-fast;

  &:hover {
    color: $color-text-primary;
    background: rgba(255, 255, 255, 0.05);
  }
}

.header-title {
  font-size: $text-lg;
  font-weight: $weight-semibold;
  color: $color-text-primary;
}

// 加载/错误状态
.loading-state,
.error-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: $spacing-md;
  color: $color-text-tertiary;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid rgba(212, 168, 83, 0.3);
  border-top-color: $color-accent;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.error-icon {
  font-size: $text-3xl;
  color: $color-error;
}

// 内容区
.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: $spacing-lg;
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
}

// 摘要栏
.summary-bar {
  display: flex;
  gap: $spacing-xl;
  padding: $spacing-lg;
  background: $color-bg-secondary;
  border-radius: $radius-lg;
}

.summary-score {
  flex-shrink: 0;
}

.big-score-ring {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: conic-gradient($color-accent calc(var(--score) * 3.6deg), rgba(255, 255, 255, 0.1) calc(var(--score) * 3.6deg));
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    inset: 6px;
    background: $color-bg-secondary;
    border-radius: 50%;
  }

  .score-value {
    position: relative;
    z-index: 1;
    font-family: $font-display;
    font-size: $text-2xl;
    font-weight: $weight-bold;
    color: $color-accent;
    line-height: 1;
  }

  .score-label {
    position: relative;
    z-index: 1;
    font-size: $text-xs;
    color: $color-text-tertiary;
    margin-top: 2px;
  }
}

.summary-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
  justify-content: center;
}

.info-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: $spacing-sm;
}

.info-label {
  font-size: $text-sm;
  color: $color-text-primary;
  font-weight: $weight-medium;
}

.info-tag {
  padding: 2px 8px;
  font-size: $text-xs;
  border-radius: $radius-sm;
  background: rgba(212, 168, 83, 0.15);
  color: $color-accent;

  &.tag-blue {
    background: rgba(96, 165, 250, 0.15);
    color: #60a5fa;
  }
}

.info-item {
  font-size: $text-sm;
  color: $color-text-secondary;

  &.performance {
    padding: 2px 8px;
    border-radius: $radius-sm;
    font-size: $text-xs;
    font-weight: $weight-medium;
  }

  &.perf-excellent {
    background: rgba(52, 211, 153, 0.15);
    color: $color-success;
  }

  &.perf-good {
    background: rgba(96, 165, 250, 0.15);
    color: #60a5fa;
  }

  &.perf-average {
    background: rgba(251, 191, 36, 0.15);
    color: $color-warning;
  }

  &.perf-poor {
    background: rgba(248, 113, 113, 0.15);
    color: $color-error;
  }
}

// Tabs
.tabs {
  display: flex;
  gap: $spacing-xs;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-sm $spacing-md;
  font-size: $text-sm;
  color: $color-text-tertiary;
  border-radius: $radius-md;
  transition: all $transition-fast;

  &:hover {
    color: $color-text-secondary;
    background: rgba(255, 255, 255, 0.03);
  }

  &.active {
    background: $color-accent-glow;
    color: $color-accent;
  }
}

// Tab 内容
.tab-content {
  flex: 1;
  min-height: 0;
}

.tab-panel {
  padding: $spacing-md;
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  max-height: calc(85vh - 320px);
  overflow-y: auto;
}

.no-data {
  text-align: center;
  padding: $spacing-2xl;
  color: $color-text-tertiary;
}

// 动画
.modal-enter-active {
  transition: opacity 0.2s ease;

  .modal-container {
    transition: transform 0.2s ease;
  }
}

.modal-leave-active {
  transition: opacity 0.15s ease;

  .modal-container {
    transition: transform 0.15s ease;
  }
}

.modal-enter-from {
  opacity: 0;

  .modal-container {
    transform: scale(0.95);
  }
}

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
