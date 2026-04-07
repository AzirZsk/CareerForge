<template>
  <div class="audio-upload-area">
    <h4>面试过程记录</h4>
    <p class="hint">请输入面试过程中的问题、回答、面试官反馈等内容，或上传面试录音自动转录</p>

    <!-- 音频上传区域 -->
    <div
      class="upload-zone"
      :class="{ 'drag-over': isDragOver }"
      @drop.prevent="handleDrop"
      @dragover.prevent="isDragOver = true"
      @dragleave.prevent="isDragOver = false"
    >
      <input
        ref="fileInput"
        type="file"
        accept="audio/*,.wav,.mp3,.m4a,.aac,.ogg,.flac,.opus,.webm,.mov,.mp4"
        class="hidden-input"
        @change="handleFileSelect"
      />

      <!-- 空闲状态 -->
      <div v-if="transcribeState.status === 'idle'" class="upload-prompt">
        <div class="upload-icon">🎵</div>
        <p>拖拽音频文件到此处，或</p>
        <button class="btn btn-secondary" @click="triggerFileSelect">点击上传</button>
        <p class="format-hint">支持 wav/mp3/m4a/aac/ogg/flac 等格式，最大 50MB</p>
      </div>

      <!-- 上传中状态 -->
      <div v-else-if="transcribeState.status === 'uploading'" class="upload-status">
        <div class="upload-icon spinning">⏳</div>
        <p>{{ transcribeState.message }}</p>
      </div>

      <!-- 转录中状态 -->
      <div v-else-if="transcribeState.status === 'processing'" class="upload-status">
        <div class="progress-ring">
          <svg viewBox="0 0 36 36">
            <circle cx="18" cy="18" r="16" class="bg" />
            <circle
              cx="18"
              cy="18"
              r="16"
              class="progress"
              :style="{ strokeDashoffset: 100 - transcribeState.progress }"
            />
          </svg>
          <span class="progress-text">{{ transcribeState.progress }}%</span>
        </div>
        <p>{{ transcribeState.message }}</p>
        <button class="btn btn-sm btn-secondary" @click="cancelTranscribe">取消</button>
      </div>

      <!-- 转录完成 -->
      <div v-else-if="transcribeState.status === 'completed'" class="upload-status success">
        <div class="upload-icon">✅</div>
        <p>转录完成</p>
        <div class="transcript-preview" v-if="transcribeState.transcriptText">
          <p class="preview-text">{{ previewText }}</p>
        </div>
        <div class="status-actions">
          <button class="btn btn-sm btn-ai" @click="applyTranscript">应用转录结果</button>
          <button class="btn btn-sm btn-secondary" @click="resetTranscribe">重新上传</button>
        </div>
      </div>

      <!-- 转录失败 -->
      <div v-else-if="transcribeState.status === 'failed'" class="upload-status error">
        <div class="upload-icon">❌</div>
        <p>{{ transcribeState.error || '转录失败' }}</p>
        <button class="btn btn-sm btn-secondary" @click="resetTranscribe">重试</button>
      </div>
    </div>

    <!-- 文本输入区域 -->
    <div class="text-input-area">
      <div class="input-header">
        <span>或直接输入/编辑文本</span>
        <span v-if="charCount" class="char-count">{{ charCount }} 字</span>
      </div>
      <textarea
        :value="displayText"
        @input="handleTextInput"
        placeholder="例如：&#10;1. 面试官：请介绍一下你的项目经验？&#10;我：我负责过 xxx 项目...&#10;&#10;2. 面试官：你遇到过什么技术难题？&#10;我：..."
        rows="6"
        class="transcript-textarea"
      ></textarea>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useAudioTranscribe } from '@/composables/useAudioTranscribe'

const props = defineProps<{
  interviewId: string
  modelValue: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const {
  state: transcribeState,
  transcribe,
  cancel,
  reset,
  validateFile
} = useAudioTranscribe(props.interviewId)

const fileInput = ref<HTMLInputElement>()
const isDragOver = ref(false)
const localText = ref('')

// 显示文本：优先显示转录结果，否则显示本地编辑或传入值
const displayText = computed(() => {
  if (transcribeState.transcriptText) {
    return transcribeState.transcriptText
  }
  return localText.value || props.modelValue
})

// 字符计数
const charCount = computed(() => {
  return displayText.value.length || 0
})

// 转录文本预览（前 200 字符）
const previewText = computed(() => {
  const text = transcribeState.transcriptText
  if (text.length <= 200) return text
  return text.substring(0, 200) + '...'
})

function triggerFileSelect() {
  fileInput.value?.click()
}

function handleFileSelect(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {
    processFile(file)
  }
}

function handleDrop(event: DragEvent) {
  isDragOver.value = false
  const file = event.dataTransfer?.files[0]
  if (file && file.type.startsWith('audio/')) {
    processFile(file)
  }
}

function processFile(file: File) {
  const error = validateFile(file)
  if (error) {
    alert(error)
    return
  }
  transcribe(file)
}

function handleTextInput(event: Event) {
  const target = event.target as HTMLTextAreaElement
  localText.value = target.value
  emit('update:modelValue', target.value)
}

function applyTranscript() {
  emit('update:modelValue', transcribeState.transcriptText)
  reset()
}

function cancelTranscribe() {
  cancel()
}

function resetTranscribe() {
  reset()
}

// 监听转录完成，自动填充
watch(() => transcribeState.status, (status) => {
  if (status === 'completed' && transcribeState.transcriptText) {
    // 可以选择自动填充
    // emit('update:modelValue', transcribeState.transcriptText)
  }
})
</script>

<style scoped lang="scss">
.audio-upload-area {
  margin-bottom: $spacing-lg;

  h4 {
    font-size: 0.875rem;
    font-weight: 600;
    color: $color-text-primary;
    margin-bottom: $spacing-xs;
  }

  .hint {
    font-size: 0.75rem;
    color: $color-text-tertiary;
    margin-bottom: $spacing-sm;
  }
}

.upload-zone {
  border: 2px dashed $color-bg-elevated;
  border-radius: $radius-md;
  padding: $spacing-lg;
  text-align: center;
  transition: all 0.2s;
  margin-bottom: $spacing-md;
  background: $color-bg-tertiary;

  &:hover {
    border-color: $color-accent;
    background: rgba($color-accent, 0.05);
  }

  &.drag-over {
    border-color: $color-accent;
    background: rgba($color-accent, 0.1);
  }
}

.hidden-input {
  display: none;
}

.upload-prompt {
  .upload-icon {
    font-size: 2.5rem;
    margin-bottom: $spacing-sm;
  }

  p {
    color: $color-text-secondary;
    margin-bottom: $spacing-sm;
  }

  .format-hint {
    font-size: 0.75rem;
    color: $color-text-tertiary;
    margin-top: $spacing-sm;
  }
}

.upload-status {
  .upload-icon {
    font-size: 2.5rem;
    margin-bottom: $spacing-sm;

    &.spinning {
      animation: spin 1s linear infinite;
    }
  }

  p {
    color: $color-text-secondary;
    margin-bottom: $spacing-sm;
  }

  &.success {
    .upload-icon {
      color: $color-success;
    }
  }

  &.error {
    .upload-icon {
      color: $color-error;
    }
  }
}

.progress-ring {
  position: relative;
  width: 60px;
  height: 60px;
  margin: 0 auto $spacing-sm;

  svg {
    transform: rotate(-90deg);
  }

  circle {
    fill: none;
    stroke-width: 3;

    &.bg {
      stroke: $color-bg-elevated;
    }

    &.progress {
      stroke: $color-accent;
      stroke-linecap: round;
      stroke-dasharray: 100;
      transition: stroke-dashoffset 0.3s;
    }
  }

  .progress-text {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    font-size: 0.875rem;
    font-weight: 600;
    color: $color-accent;
  }
}

.transcript-preview {
  margin-top: $spacing-sm;
  padding: $spacing-sm;
  background: $color-bg-secondary;
  border-radius: $radius-sm;

  .preview-text {
    font-size: 0.75rem;
    color: $color-text-secondary;
    text-align: left;
    line-height: 1.5;
    margin: 0;
  }
}

.status-actions {
  display: flex;
  gap: $spacing-sm;
  justify-content: center;
}

.text-input-area {
  .input-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: $spacing-xs;
    font-size: 0.75rem;
    color: $color-text-tertiary;

    .char-count {
      color: $color-text-tertiary;
    }
  }

  .transcript-textarea {
    width: 100%;
    padding: $spacing-sm;
    background: $color-bg-tertiary;
    border: 1px solid $color-bg-elevated;
    border-radius: $radius-sm;
    color: $color-text-primary;
    font-size: 0.875rem;
    line-height: 1.5;
    resize: vertical;
    min-height: 120px;

    &:focus {
      outline: none;
      border-color: $color-accent;
    }

    &::placeholder {
      color: $color-text-tertiary;
    }
  }
}

.btn-ai {
  background: linear-gradient(135deg, $color-accent 0%, $color-accent-dark 100%);
  color: $color-bg-primary;
  border: none;
  font-weight: 500;

  &:hover {
    opacity: 0.9;
    transform: translateY(-1px);
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
</style>
