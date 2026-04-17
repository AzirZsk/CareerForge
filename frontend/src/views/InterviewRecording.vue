<template>
  <div class="interview-recording-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <button class="back-btn" @click="goBack">
        <font-awesome-icon icon="fa-solid fa-arrow-left" class="icon" />
        <span>返回复盘</span>
      </button>
      <h1>面试录音回放</h1>
      <div class="header-actions">
        <button class="action-btn" @click="downloadAudio" :disabled="!recordingInfo">
          <font-awesome-icon icon="fa-solid fa-download" class="icon" />
          下载录音
        </button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="isLoading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>正在加载录音信息...</p>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-state">
      <font-awesome-icon icon="fa-solid fa-circle-exclamation" class="icon" />
      <p>{{ error }}</p>
      <button class="retry-btn" @click="loadRecordingInfo">重试</button>
    </div>

    <!-- 主内容区 -->
    <div v-else-if="recordingInfo" class="recording-content">
      <!-- 播放器区域 -->
      <div class="player-section">
        <RecordingPlayer
          :audio-url="recordingInfo.mergedAudioUrl"
          :segments="recordingInfo.segments"
          :total-duration-ms="recordingInfo.totalDurationMs"
          @timeupdate="handleTimeUpdate"
          @segmentchange="handleSegmentChange"
        />
      </div>

      <!-- 文字记录区域 -->
      <div class="transcript-section">
        <TranscriptViewer
          :transcript="recordingInfo.transcript"
          :current-time="currentTime"
          :current-segment-index="currentSegmentIndex"
          @jump-to-segment="handleJumpToSegment"
        />
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <font-awesome-icon icon="fa-solid fa-microphone" class="icon" />
      <p>暂无录音数据</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import RecordingPlayer from '@/components/interview/recording/RecordingPlayer.vue'
import TranscriptViewer from '@/components/interview/recording/TranscriptViewer.vue'
import { getRecordingInfo, getMergedAudio } from '@/api/interview-voice'
import type { RecordingInfo } from '@/types/interview-voice'

// 路由
const route = useRoute()
const router = useRouter()

// 状态
const isLoading = ref(true)
const error = ref<string | null>(null)
const recordingInfo = ref<RecordingInfo | null>(null)
const currentTime = ref(0)
const currentSegmentIndex = ref(0)

// 获取会话 ID（路由参数名是 sessionId）
const sessionId = route.params.sessionId as string || route.query.sessionId as string

// 加载录音信息
async function loadRecordingInfo(): Promise<void> {
  if (!sessionId) {
    error.value = '缺少会话 ID'
    isLoading.value = false
    return
  }

  isLoading.value = true
  error.value = null

  try {
    recordingInfo.value = await getRecordingInfo(sessionId)
  } catch (e) {
    console.error('[InterviewRecording] Failed to load recording info:', e)
    error.value = e instanceof Error ? e.message : '加载录音信息失败'
  } finally {
    isLoading.value = false
  }
}

// 返回面试详情页
function goBack(): void {
  const interviewId = route.query.interviewId as string
  if (interviewId) {
    router.push(`/interview-center/${interviewId}`)
  } else {
    router.push('/interview-center')
  }
}

// 下载音频
async function downloadAudio(): Promise<void> {
  if (!sessionId) return
  try {
    const audioUrl = await getMergedAudio(sessionId)
    const link = document.createElement('a')
    link.href = audioUrl
    link.download = `interview_${sessionId}.wav`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    // 释放 Blob URL
    URL.revokeObjectURL(audioUrl)
  } catch (e) {
    console.error('[InterviewRecording] Failed to download audio:', e)
  }
}

// 处理时间更新
function handleTimeUpdate(time: number): void {
  currentTime.value = time
}

// 处理片段切换
function handleSegmentChange(index: number): void {
  currentSegmentIndex.value = index
}

// 处理跳转到片段（由 TranscriptViewer 触发，需要通知 RecordingPlayer）
function handleJumpToSegment(index: number): void {
  currentSegmentIndex.value = index
}

// 初始化
onMounted(() => {
  loadRecordingInfo()
})
</script>

<style lang="scss" scoped>
.interview-recording-page {
  min-height: 100vh;
  background: $color-bg-primary;
  padding: $spacing-xl;
}

.page-header {
  display: flex;
  align-items: center;
  gap: $spacing-lg;
  margin-bottom: $spacing-xl;

  h1 {
    font-size: 1.5rem;
    font-weight: 600;
    color: $color-text-primary;
    margin: 0;
    flex: 1;
  }
}

.back-btn {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-sm $spacing-md;
  border: none;
  border-radius: $radius-md;
  background: $color-bg-secondary;
  color: $color-text-secondary;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: $color-bg-tertiary;
    color: $color-text-primary;
  }

  .icon {
    font-size: 1.25rem;
  }
}

.header-actions {
  display: flex;
  gap: $spacing-sm;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-sm $spacing-md;
  border: none;
  border-radius: $radius-md;
  background: $color-accent;
  color: $color-bg-deep;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover:not(:disabled) {
    background: $color-accent-light;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .icon {
    font-size: 1rem;
  }
}

.recording-content {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: $spacing-xl;
  max-width: 1400px;
  margin: 0 auto;
}

.transcript-section {
  height: calc(100vh - 200px);
  min-height: 500px;
}

.loading-state,
.error-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  color: $color-text-tertiary;

  .icon {
    font-size: 3rem;
    margin-bottom: $spacing-md;
  }

  p {
    font-size: 1rem;
    margin-bottom: $spacing-md;
  }
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 3px solid $color-bg-tertiary;
  border-top-color: $color-accent;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: $spacing-md;
}

.error-state {
  color: $color-error;
}

.retry-btn {
  padding: $spacing-sm $spacing-lg;
  border: none;
  border-radius: $radius-md;
  background: $color-accent;
  color: $color-bg-deep;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: $color-accent-light;
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1024px) {
  .recording-content {
    grid-template-columns: 1fr;
  }

  .transcript-section {
    height: 400px;
  }
}
</style>
