<!--=====================================================
  模拟面试历史记录区域
  支持自动检测异步分析状态、三态标签、轮询刷新
  @author Azir
=====================================================-->

<template>
  <div class="mock-history">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="spinner" />
      <span>加载中...</span>
    </div>

    <!-- 空状态 -->
    <div v-else-if="sessions.length === 0" class="empty-state">
      <div class="empty-icon">
        <font-awesome-icon icon="fa-solid fa-microphone" />
      </div>
      <p class="empty-text">还没有模拟面试记录</p>
      <p class="empty-hint">点击页面上方「开始模拟面试」按钮进行练习</p>
    </div>

    <!-- 记录列表 -->
    <div v-else class="session-list">
      <div
        v-for="(session, index) in sessions"
        :key="session.id"
        class="session-card"
        :style="{ '--index': index }"
      >
        <div class="card-left">
          <!-- 评分环 -->
          <div class="score-ring" :style="{ '--score': session.overallScore || 0 }">
            <span>{{ session.overallScore ?? '-' }}</span>
          </div>
        </div>

        <div class="card-center">
          <div class="card-meta">
            <span class="meta-item">
              <font-awesome-icon icon="fa-regular fa-clock" />
              {{ formatDate(session.createdAt) }}
            </span>
            <span class="meta-item">
              <font-awesome-icon icon="fa-solid fa-hourglass-half" />
              {{ session.duration ? `${session.duration}分钟` : '--' }}
            </span>
            <span class="meta-item">
              <font-awesome-icon icon="fa-solid fa-list-ol" />
              {{ session.questions || 0 }}题
            </span>
            <span class="meta-item">
              <font-awesome-icon icon="fa-solid fa-hand" />
              求助 {{ session.assistCount ?? 0 }}/{{ session.assistLimit ?? 0 }}
            </span>
          </div>
          <div class="card-tags">
            <span class="tag tag-style">{{ styleLabel(session.interviewerStyle) }}</span>
            <span class="tag tag-mode">{{ modeLabel(session.voiceMode) }}</span>
            <span v-if="session.hasAnalysis" class="tag tag-analysis">已分析</span>
            <span v-else-if="isAnalyzing(session.id)" class="tag tag-analyzing">
              <span class="analyzing-dot"></span>
              分析中
            </span>
            <span v-else class="tag tag-pending">待分析</span>
          </div>
        </div>

        <div class="card-actions">
          <button
            v-if="isAnalyzing(session.id)"
            class="btn btn-detail btn-analyzing"
            disabled
          >
            <div class="btn-spinner"></div>
            分析中...
          </button>
          <button
            v-else
            class="btn btn-detail"
            :disabled="!session.hasAnalysis"
            @click="emit('openDetail', session.id)"
          >
            <font-awesome-icon icon="fa-solid fa-chart-bar" />
            查看分析
          </button>
          <button
            v-if="session.hasRecording"
            class="btn btn-recording"
            @click="goRecording(session.sessionId)"
          >
            <font-awesome-icon icon="fa-solid fa-play" />
            录音回放
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMockSessions } from '@/api/interview-center'
import { getTaskByBusinessId } from '@/api/task'
import type { MockSessionSummary } from '@/types/interview-center'

const props = defineProps<{
  interviewId: string
}>()

const emit = defineEmits<{
  (e: 'openDetail', mockInterviewId: string): void
}>()

const router = useRouter()
const sessions = ref<MockSessionSummary[]>([])
const loading = ref(true)

// 异步分析状态追踪
const analyzingMockId = ref<string | null>(null)
let pollingTimer: ReturnType<typeof setInterval> | null = null

onMounted(async () => {
  await loadSessions()
  // 加载完成后检查是否有正在进行的分析任务
  checkPendingAnalysis()
})

onUnmounted(() => {
  stopPolling()
})

async function loadSessions() {
  loading.value = true
  try {
    sessions.value = await getMockSessions(props.interviewId)
  } catch (e) {
    console.error('加载模拟面试历史失败:', e)
  } finally {
    loading.value = false
  }
}

/**
 * 检查是否有正在进行的异步分析任务
 * 找到最新一条 hasAnalysis=false 的记录，查询其关联的 AsyncTask
 */
async function checkPendingAnalysis() {
  // 找最新的未分析记录
  const pending = sessions.value.find(s => !s.hasAnalysis)
  if (!pending) return
  try {
    const task = await getTaskByBusinessId({
      businessId: pending.id,
      taskType: 'review_analysis'
    })
    if (task && (task.status === 'pending' || task.status === 'running')) {
      analyzingMockId.value = pending.id
      startPolling()
    }
  } catch (e) {
    console.warn('检查异步分析任务失败:', e)
  }
}

function startPolling() {
  if (pollingTimer) return
  pollingTimer = setInterval(async () => {
    if (!analyzingMockId.value) {
      stopPolling()
      return
    }
    try {
      const task = await getTaskByBusinessId({
        businessId: analyzingMockId.value,
        taskType: 'review_analysis'
      })
      if (!task || task.status === 'completed' || task.status === 'failed') {
        stopPolling()
        // 刷新列表（分析完成或失败，列表数据需要更新）
        await loadSessions()
        if (task?.status === 'failed') {
          analyzingMockId.value = null
        }
      }
    } catch (e) {
      console.error('轮询分析任务状态失败:', e)
      stopPolling()
    }
  }, 5000)
}

function stopPolling() {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

function isAnalyzing(mockId: string): boolean {
  return analyzingMockId.value === mockId
}

/**
 * 刷新列表（供父组件调用）
 */
async function refresh() {
  stopPolling()
  analyzingMockId.value = null
  await loadSessions()
  checkPendingAnalysis()
}

defineExpose({ refresh })

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

function goRecording(sessionId: string): void {
  router.push({
    name: 'MockInterviewRecording',
    params: { sessionId },
    query: { interviewId: props.interviewId }
  })
}
</script>

<style lang="scss" scoped>
.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-sm;
  padding: $spacing-xl;
  color: $color-text-tertiary;
}

.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(212, 168, 83, 0.3);
  border-top-color: $color-accent;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.empty-state {
  text-align: center;
  padding: $spacing-2xl;
}

.empty-icon {
  width: 48px;
  height: 48px;
  margin: 0 auto $spacing-md;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.05);
  border-radius: $radius-lg;
  font-size: $text-xl;
  color: $color-text-tertiary;
}

.empty-text {
  font-size: $text-base;
  color: $color-text-secondary;
  margin-bottom: $spacing-xs;
}

.empty-hint {
  font-size: $text-sm;
  color: $color-text-tertiary;
}

.session-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.session-card {
  display: flex;
  align-items: center;
  gap: $spacing-lg;
  padding: $spacing-lg;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: $radius-lg;
  animation: slideUp 0.4s ease forwards;
  animation-delay: calc(var(--index) * 0.08s);
  opacity: 0;
  transition: border-color $transition-fast;

  &:hover {
    border-color: rgba(212, 168, 83, 0.15);
  }
}

.card-left {
  flex-shrink: 0;
}

.score-ring {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: conic-gradient($color-accent calc(var(--score) * 3.6deg), rgba(255, 255, 255, 0.1) calc(var(--score) * 3.6deg));
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    inset: 5px;
    background: $color-bg-tertiary;
    border-radius: 50%;
  }

  span {
    position: relative;
    z-index: 1;
    font-family: $font-display;
    font-size: $text-lg;
    font-weight: $weight-semibold;
    color: $color-accent;
  }
}

.card-center {
  flex: 1;
  min-width: 0;
}

.card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-md;
  margin-bottom: $spacing-sm;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $text-sm;
  color: $color-text-secondary;
}

.card-tags {
  display: flex;
  gap: $spacing-xs;
}

.tag {
  padding: 2px 8px;
  font-size: $text-xs;
  border-radius: $radius-sm;

  &.tag-style {
    background: rgba(212, 168, 83, 0.15);
    color: $color-accent;
  }

  &.tag-mode {
    background: rgba(96, 165, 250, 0.15);
    color: #60a5fa;
  }

  &.tag-analysis {
    background: rgba(52, 211, 153, 0.15);
    color: $color-success;
  }

  &.tag-analyzing {
    display: flex;
    align-items: center;
    gap: 4px;
    background: rgba(212, 168, 83, 0.15);
    color: $color-accent;
  }

  &.tag-pending {
    background: rgba(255, 255, 255, 0.05);
    color: $color-text-tertiary;
  }
}

.analyzing-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: $color-accent;
  animation: pulse-opacity 1.5s ease-in-out infinite;
}

.card-actions {
  display: flex;
  gap: $spacing-sm;
  flex-shrink: 0;
}

.btn {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-xs $spacing-md;
  font-size: $text-xs;
  font-weight: $weight-medium;
  border-radius: $radius-sm;
  transition: all $transition-fast;

  &.btn-detail {
    background: $color-accent-glow;
    color: $color-accent;

    &:hover:not(:disabled) {
      background: rgba(212, 168, 83, 0.2);
    }

    &:disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }
  }

  &.btn-analyzing {
    gap: 6px;
  }

  &.btn-recording {
    background: rgba(96, 165, 250, 0.15);
    color: #60a5fa;

    &:hover {
      background: rgba(96, 165, 250, 0.25);
    }
  }
}

.btn-spinner {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(212, 168, 83, 0.3);
  border-top-color: $color-accent;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes pulse-opacity {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}
</style>
