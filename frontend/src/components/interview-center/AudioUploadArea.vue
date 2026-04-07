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
      <div v-if="!isUploading" class="upload-prompt">
        <p>拖拽音频文件到此处，或</p>
        <button class="btn btn-secondary" @click="triggerFileSelect">点击上传</button>
        <span class="format-hint">（wav/mp3/m4a 等，最大 50MB）</span>
      </div>

      <!-- 上传中状态 -->
      <div v-else class="upload-status">
        <span class="upload-icon spinning">⏳</span>
        <span>正在上传...</span>
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
import { ref, computed } from 'vue'
import { useNotificationStore } from '@/stores/notification'
import { useToast } from '@/composables/useToast'

// 支持的音频格式
const SUPPORTED_FORMATS = ['wav', 'mp3', 'm4a', 'aac', 'ogg', 'flac', 'opus', 'webm', 'mov', 'mp4']
const MAX_FILE_SIZE_MB = 50

const props = defineProps<{
  interviewId: string
  modelValue: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const notificationStore = useNotificationStore()
const toast = useToast()

const fileInput = ref<HTMLInputElement>()
const isDragOver = ref(false)
const localText = ref('')
const isUploading = ref(false)

// 显示文本：本地编辑或传入值
const displayText = computed(() => {
  return localText.value || props.modelValue
})

// 字符计数
const charCount = computed(() => {
  return displayText.value.length || 0
})

/**
 * 验证文件格式和大小
 */
function validateFile(file: File): string | null {
  const ext = getFileExtension(file.name)
  if (!ext || !SUPPORTED_FORMATS.includes(ext)) {
    return `不支持的文件格式，支持: ${SUPPORTED_FORMATS.join(', ')}`
  }
  if (file.size > MAX_FILE_SIZE_MB * 1024 * 1024) {
    return `文件大小超过 ${MAX_FILE_SIZE_MB}MB 限制`
  }
  return null
}

/**
 * 获取文件扩展名
 */
function getFileExtension(filename: string): string {
  if (!filename) return ''
  const dotIndex = filename.lastIndexOf('.')
  return dotIndex > 0 ? filename.substring(dotIndex + 1).toLowerCase() : ''
}

function triggerFileSelect() {
  fileInput.value?.click()
}

function handleFileSelect(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {
    processFile(file)
  }
  // 清空 input 以便重复选择同一文件
  target.value = ''
}

function handleDrop(event: DragEvent) {
  isDragOver.value = false
  const file = event.dataTransfer?.files[0]
  if (file && file.type.startsWith('audio/')) {
    processFile(file)
  }
}

async function processFile(file: File) {
  const error = validateFile(file)
  if (error) {
    toast.error(error)
    return
  }

  isUploading.value = true
  try {
    const taskId = await notificationStore.createAudioTranscribeTask(
      props.interviewId,
      file
    )
    if (taskId) {
      toast.success('音频已提交，转录任务已创建，请在消息中心查看进度')
    } else {
      toast.error('创建转录任务失败')
    }
  } catch (e) {
    console.error('[AudioUploadArea] 创建任务失败:', e)
    toast.error('创建转录任务失败')
  } finally {
    isUploading.value = false
  }
}

function handleTextInput(event: Event) {
  const target = event.target as HTMLTextAreaElement
  localText.value = target.value
  emit('update:modelValue', target.value)
}
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
  border: 1.5px dashed $color-bg-elevated;
  border-radius: $radius-sm;
  padding: $spacing-sm $spacing-md;
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
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-sm;
  flex-wrap: wrap;

  .upload-icon {
    font-size: 1.25rem;
  }

  p {
    color: $color-text-secondary;
    font-size: 0.8125rem;
    margin: 0;
  }

  .format-hint {
    font-size: 0.6875rem;
    color: $color-text-tertiary;
  }

  .task-hint {
    display: none;
  }
}

.upload-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-xs;

  .upload-icon.spinning {
    display: inline-block;
    animation: spin 1s linear infinite;
  }

  span {
    color: $color-text-secondary;
    font-size: 0.8125rem;
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


@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
