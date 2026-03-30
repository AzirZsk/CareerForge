<template>
  <div class="voice-controls">
    <!-- 语音模式切换 -->
    <div class="mode-switch">
      <button
        v-for="mode in voiceModes"
        :key="mode.value"
        :class="['mode-btn', { active: currentMode === mode.value }]"
        @click="switchMode(mode.value)"
      >
        {{ mode.label }}
      </button>
    </div>

    <!-- 录音控制 -->
    <div class="recording-controls">
      <button
        :class="['record-btn', { recording: isRecording }]"
        @click="toggleRecording"
        :disabled="isProcessing"
      >
        <span class="icon">{{ isRecording ? '⏹️' : '🎤' }}</span>
        <span class="text">{{ isRecording ? '停止' : '录音' }}</span>
      </button>

      <!-- 状态指示器 -->
      <div v-if="statusText" class="status-indicator">
        <span class="status-dot" :class="statusClass"></span>
        <span class="status-text">{{ statusText }}</span>
      </div>
    </div>

    <!-- 音量控制 -->
    <div class="volume-control">
      <label>音量</label>
      <input
        type="range"
        min="0"
        max="100"
        :value="volume * 100"
        @input="updateVolume(Number(($event.target as HTMLInputElement).value) / 100)"
      />
      <button @click="toggleMute" :class="{ muted: isMuted }">
        {{ isMuted ? '🔇' : '🔊' }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { VoiceMode } from '@/types/interview-voice'

// Props
interface Props {
  modelValue?: VoiceMode
  isRecording?: boolean
  isProcessing?: boolean
  statusText?: string
  statusType?: 'idle' | 'recording' | 'recognizing' | 'synthesizing' | 'error'
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: 'half_voice',
  isRecording: false,
  isProcessing: false,
  statusText: '',
  statusType: 'idle'
})

// Emits - 使用传统函数签名语法
const emit = defineEmits<{
  (e: 'update:modelValue', mode: VoiceMode): void
  (e: 'toggle-recording'): void
  (e: 'volume-change', volume: number): void
  (e: 'mute-change', muted: boolean): void
}>()

// 语音模式选项
const voiceModes = [
  { value: 'half_voice' as VoiceMode, label: '半语音' },
  { value: 'full_voice' as VoiceMode, label: '全语音' }
]

// 当前模式
const currentMode = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 音量
const volume = ref(0.8)
const isMuted = ref(false)

// 状态样式
const statusClass = computed(() => {
  const typeMap: Record<string, string> = {
    idle: 'status-idle',
    recording: 'status-recording',
    recognizing: 'status-recognizing',
    synthesizing: 'status-synthesizing',
    error: 'status-error'
  }
  return typeMap[props.statusType] || 'status-idle'
})

// 切换模式
function switchMode(mode: VoiceMode) {
  currentMode.value = mode
}

// 切换录音
function toggleRecording() {
  emit('toggle-recording')
}

// 更新音量
function updateVolume(value: number) {
  volume.value = value
  emit('volume-change', value)
}

// 切换静音
function toggleMute() {
  isMuted.value = !isMuted.value
  emit('mute-change', isMuted.value)
}
</script>

<style scoped lang="scss">
.voice-controls {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: 12px;
}

.mode-switch {
  display: flex;
  gap: 8px;

  .mode-btn {
    flex: 1;
    padding: 8px 16px;
    border: 1px solid var(--border-color);
    background: var(--bg-primary);
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.2s;

    &.active {
      background: var(--primary-color);
      color: white;
      border-color: var(--primary-color);
    }
  }
}

.recording-controls {
  display: flex;
  align-items: center;
  gap: 16px;

  .record-btn {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 24px;
    background: var(--primary-color);
    color: white;
    border: none;
    border-radius: 24px;
    cursor: pointer;
    font-size: 16px;
    transition: all 0.2s;

    &.recording {
      background: var(--danger-color);
      animation: pulse 1.5s infinite;
    }

    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  }

  .status-indicator {
    display: flex;
    align-items: center;
    gap: 8px;

    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;

      &.status-idle { background: var(--gray-color); }
      &.status-recording { background: var(--danger-color); animation: pulse 1s infinite; }
      &.status-recognizing { background: var(--warning-color); }
      &.status-synthesizing { background: var(--success-color); }
      &.status-error { background: var(--danger-color); }
    }
  }
}

.volume-control {
  display: flex;
  align-items: center;
  gap: 12px;

  input[type="range"] {
    flex: 1;
    height: 4px;
    background: var(--border-color);
    border-radius: 2px;
    appearance: none;

    &::-webkit-slider-thumb {
      appearance: none;
      width: 16px;
      height: 16px;
      background: var(--primary-color);
      border-radius: 50%;
      cursor: pointer;
    }
  }

  button {
    padding: 8px;
    background: transparent;
    border: none;
    cursor: pointer;
    font-size: 20px;

    &.muted {
      opacity: 0.5;
    }
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
