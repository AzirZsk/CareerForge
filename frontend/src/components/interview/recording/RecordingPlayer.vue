<template>
  <div class="recording-player">
    <!-- 播放控制区 -->
    <div class="player-controls">
      <button class="control-btn" @click="skipToPrevious" :disabled="currentSegmentIndex <= 0">
        <span class="icon">⏮</span>
      </button>
      <button class="control-btn play-btn" @click="togglePlay" :disabled="isLoading">
        <span v-if="isLoading" class="icon loading">⟳</span>
        <span v-else class="icon">{{ isPlaying ? '⏸' : '▶' }}</span>
      </button>
      <button class="control-btn" @click="skipToNext" :disabled="currentSegmentIndex >= segments.length - 1">
        <span class="icon">⏭</span>
      </button>
    </div>

    <!-- 进度条 -->
    <div class="progress-section">
      <span class="time-display">{{ formatTime(currentTime) }}</span>
      <div class="progress-bar-container" @click="seekToPosition" ref="progressBarRef">
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
          <!-- 片段标记 -->
          <div
            v-for="(marker, index) in segmentMarkers"
            :key="index"
            class="segment-marker"
            :class="{ active: index === currentSegmentIndex }"
            :style="{ left: marker.startPercent + '%' }"
            @click.stop="jumpToSegment(index)"
          ></div>
        </div>
      </div>
      <span class="time-display">{{ formatTime(duration) }}</span>
    </div>

    <!-- 当前片段信息 -->
    <div class="current-segment-info" v-if="currentSegment">
      <span class="segment-role" :class="currentSegment.role">
        {{ getRoleName(currentSegment.role) }}
      </span>
      <span class="segment-content">{{ truncateText(currentSegment.content, 50) }}</span>
    </div>

    <!-- 隐藏的 Audio 元素 -->
    <audio ref="audioRef" @timeupdate="handleTimeUpdate" @loadedmetadata="handleLoaded" @ended="handleEnded" @error="handleError"></audio>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue'
import type { RecordingSegment } from '@/types/interview-voice'
import {
  getRoleName,
  formatTime,
  truncateText,
  calculateSegmentTimeOffsets,
  findSegmentIndexByTime
} from '@/utils/recording-helpers'

// Props
interface Props {
  audioUrl: string
  segments: RecordingSegment[]
  totalDurationMs: number
}

const props = defineProps<Props>()

// Emits
const emit = defineEmits<{
  (e: 'timeupdate', currentTime: number): void
  (e: 'segmentchange', segmentIndex: number): void
}>()

// Refs
const audioRef = ref<HTMLAudioElement | null>(null)
const progressBarRef = ref<HTMLElement | null>(null)

// 状态
const isPlaying = ref(false)
const isLoading = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const currentSegmentIndex = ref(0)

// 预计算片段时间偏移量
const segmentTimeOffsets = computed(() => calculateSegmentTimeOffsets(props.segments))

// 计算属性
const progressPercent = computed(() => {
  if (duration.value === 0) return 0
  return (currentTime.value / duration.value) * 100
})

const currentSegment = computed(() => {
  return props.segments[currentSegmentIndex.value] || null
})

// 计算片段标记位置
const segmentMarkers = computed(() => {
  if (duration.value === 0 || props.segments.length === 0) return []
  return props.segments.map((segment, index) => ({
    startPercent: (segmentTimeOffsets.value[index] / duration.value) * 100,
    segment
  }))
})

// 播放/暂停切换
function togglePlay() {
  if (!audioRef.value) return
  if (isPlaying.value) {
    audioRef.value.pause()
    isPlaying.value = false
  } else {
    audioRef.value.play()
    isPlaying.value = true
  }
}

// 跳转到指定位置
function seekToPosition(event: MouseEvent) {
  if (!audioRef.value || !progressBarRef.value) return
  const rect = progressBarRef.value.getBoundingClientRect()
  const percent = (event.clientX - rect.left) / rect.width
  const newTime = percent * duration.value
  audioRef.value.currentTime = newTime
  currentTime.value = newTime
  updateCurrentSegment()
}

// 跳转到上一片段
function skipToPrevious() {
  if (currentSegmentIndex.value > 0) {
    jumpToSegment(currentSegmentIndex.value - 1)
  }
}

// 跳转到下一片段
function skipToNext() {
  if (currentSegmentIndex.value < props.segments.length - 1) {
    jumpToSegment(currentSegmentIndex.value + 1)
  }
}

// 跳转到指定片段
function jumpToSegment(index: number) {
  if (!audioRef.value || index < 0 || index >= props.segments.length) return
  const targetTime = segmentTimeOffsets.value[index]
  audioRef.value.currentTime = targetTime
  currentTime.value = targetTime
  currentSegmentIndex.value = index
  emit('segmentchange', index)
}

// 更新当前片段索引
function updateCurrentSegment() {
  const newIndex = findSegmentIndexByTime(props.segments, currentTime.value)
  if (currentSegmentIndex.value !== newIndex) {
    currentSegmentIndex.value = newIndex
    emit('segmentchange', newIndex)
  }
}

// 事件处理
function handleTimeUpdate() {
  if (!audioRef.value) return
  currentTime.value = audioRef.value.currentTime
  emit('timeupdate', currentTime.value)
  updateCurrentSegment()
}

function handleLoaded() {
  if (!audioRef.value) return
  duration.value = audioRef.value.duration
  isLoading.value = false
}

function handleEnded() {
  isPlaying.value = false
}

function handleError(e: Event) {
  console.error('[RecordingPlayer] Audio error:', e)
  isLoading.value = false
  isPlaying.value = false
}

// 加载音频
async function loadAudio() {
  if (!audioRef.value || !props.audioUrl) return
  isLoading.value = true
  audioRef.value.src = props.audioUrl
  try {
    await audioRef.value.load()
  } catch (e) {
    console.error('[RecordingPlayer] Failed to load audio:', e)
    isLoading.value = false
  }
}

// 监听 audioUrl 变化
watch(() => props.audioUrl, () => {
  loadAudio()
}, { immediate: true })

// 监听 totalDurationMs 变化
watch(() => props.totalDurationMs, (newVal) => {
  if (newVal && newVal > 0) {
    duration.value = newVal / 1000
  }
})

// 清理
onUnmounted(() => {
  if (audioRef.value) {
    audioRef.value.pause()
    audioRef.value.src = ''
  }
})
</script>

<style lang="scss" scoped>
.recording-player {
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  padding: $spacing-lg;
}

.player-controls {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: $spacing-md;
  margin-bottom: $spacing-md;
}

.control-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border: none;
  border-radius: $radius-full;
  background: $color-bg-tertiary;
  color: $color-text-primary;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover:not(:disabled) {
    background: $color-bg-elevated;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  &.play-btn {
    width: 56px;
    height: 56px;
    background: $color-accent;
    color: $color-bg-deep;

    &:hover:not(:disabled) {
      background: $color-accent-light;
    }
  }

  .icon {
    font-size: 1.25rem;

    &.loading {
      animation: spin 1s linear infinite;
    }
  }
}

.progress-section {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-md;
}

.time-display {
  font-size: 0.875rem;
  color: $color-text-tertiary;
  font-family: monospace;
  min-width: 50px;

  &:last-child {
    text-align: right;
  }
}

.progress-bar-container {
  flex: 1;
  cursor: pointer;
  padding: $spacing-xs 0;
}

.progress-bar {
  position: relative;
  height: 6px;
  background: $color-bg-tertiary;
  border-radius: $radius-full;
  overflow: visible;
}

.progress-fill {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  background: $color-accent;
  border-radius: $radius-full;
  transition: width 0.1s linear;
}

.segment-marker {
  position: absolute;
  top: -3px;
  width: 12px;
  height: 12px;
  background: $color-bg-elevated;
  border: 2px solid $color-text-tertiary;
  border-radius: 50%;
  cursor: pointer;
  transform: translateX(-50%);
  transition: all 0.2s ease;

  &:hover {
    background: $color-accent;
    border-color: $color-accent;
  }

  &.active {
    background: $color-accent;
    border-color: $color-accent;
    transform: translateX(-50%) scale(1.2);
  }
}

.current-segment-info {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-md;
  background: $color-bg-tertiary;
  border-radius: $radius-md;
}

.segment-role {
  font-size: 0.75rem;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: $radius-sm;

  &.interviewer {
    background: rgba($color-accent, 0.2);
    color: $color-accent;
  }

  &.candidate {
    background: rgba($color-info, 0.2);
    color: $color-info;
  }

  &.assistant {
    background: rgba($color-success, 0.2);
    color: $color-success;
  }
}

.segment-content {
  font-size: 0.875rem;
  color: $color-text-secondary;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
