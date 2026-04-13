<template>
  <div class="voice-controls">
    <!-- 语音模式切换（面试开始后隐藏） -->
    <div v-if="!hideModeSwitch" class="mode-switch">
      <button
        v-for="mode in voiceModes"
        :key="mode.value"
        :class="['mode-btn', { active: currentMode === mode.value }]"
        @click="switchMode(mode.value)"
      >
        {{ mode.label }}
      </button>
    </div>

    <!-- 状态指示器（面试开始后显示） -->
    <div v-if="statusText" class="status-indicator">
      <span class="status-dot" :class="statusClass"></span>
      <span class="status-text">{{ statusText }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { VoiceMode } from '@/types/interview-voice'

// Props
interface Props {
  modelValue?: VoiceMode
  statusText?: string
  statusType?: 'idle' | 'recording' | 'recognizing' | 'synthesizing' | 'error'
  hideModeSwitch?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: 'half_voice',
  statusText: '',
  statusType: 'idle',
  hideModeSwitch: false
})

// Emits
const emit = defineEmits<{
  (e: 'update:modelValue', mode: VoiceMode): void
}>()

// 语音模式选项
const voiceModes = [
  { value: 'half_voice' as VoiceMode, label: '语音作答' },
  { value: 'full_voice' as VoiceMode, label: '全程对话' }
]

// 当前模式
const currentMode = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

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
</script>

<style scoped lang="scss">
.voice-controls {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
  padding: $spacing-md;
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.mode-switch {
  display: flex;
  gap: $spacing-sm;

  .mode-btn {
    flex: 1;
    padding: $spacing-sm $spacing-md;
    border: 1px solid $color-border;
    background: $color-bg-primary;
    border-radius: $radius-md;
    color: $color-text-secondary;
    cursor: pointer;
    font-size: $text-sm;
    transition: all $transition-fast;

    &:hover:not(.active) {
      border-color: rgba(255, 255, 255, 0.15);
      background: $color-bg-tertiary;
    }

    &.active {
      background: $color-accent;
      color: $color-bg-deep;
      border-color: $color-accent;
      font-weight: $weight-medium;
    }
  }
}

.status-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-sm;
  padding: $spacing-md;
  background: $color-bg-tertiary;
  border-radius: $radius-md;

  .status-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;

    &.status-idle { background: $color-text-tertiary; }
    &.status-recording { background: $color-error; animation: pulse 1s infinite; }
    &.status-recognizing { background: $color-warning; }
    &.status-synthesizing { background: $color-success; }
    &.status-error { background: $color-error; }
  }

  .status-text {
    font-size: $text-sm;
    color: $color-text-secondary;
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
