<template>
  <div class="audio-upload-area">
    <!-- 只读模式：有转译文本且非编辑状态 -->
    <div v-if="hasTranscript && !isEditing" class="transcript-display">
      <div class="display-header">
        <h4>面试过程记录</h4>
        <button class="edit-btn" @click="startEditing" title="编辑">
          <font-awesome-icon icon="fa-solid fa-pen-to-square" />
          <span>编辑</span>
        </button>
      </div>
      <div class="transcript-content">
        <p class="transcript-text">{{ modelValue }}</p>
      </div>
    </div>

    <!-- 编辑模式：无转译文本或正在编辑 -->
    <template v-else>
      <h4>面试过程记录</h4>
      <p class="hint">请输入面试过程中的问题、回答、面试官反馈等内容，或上传面试录音自动转录</p>

      <!-- 音频上传区域 -->
      <div
        class="upload-zone"
        :class="{ 'drag-over': isDragOver, 'uploading': isUploading }"
        @click="triggerFileSelect"
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
          <p>拖拽音频文件到此处，或点击上传</p>
          <span class="format-hint">（wav/mp3/m4a 等，最大 50MB）</span>
        </div>

        <!-- 上传中状态 -->
        <div v-else class="upload-status">
          <font-awesome-icon icon="fa-solid fa-hourglass-half" class="upload-icon spinning" />
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

      <!-- 编辑模式下的操作按钮（有内容时显示） -->
      <div v-if="hasTranscript || localText.trim()" class="edit-actions">
        <button class="btn btn-sm btn-secondary" @click="cancelEditing">取消</button>
        <button class="btn btn-sm btn-primary" @click="saveEditing">保存</button>
      </div>
    </template>
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
  'save': [value: string]
}>()

const notificationStore = useNotificationStore()
const toast = useToast()

const fileInput = ref<HTMLInputElement>()
const isDragOver = ref(false)
const localText = ref('')
const isUploading = ref(false)
const isEditing = ref(false)

// 是否有转译文本
const hasTranscript = computed(() => {
  return !!props.modelValue?.trim()
})

// 显示文本：本地编辑或传入值
const displayText = computed(() => {
  return localText.value || props.modelValue
})

// 字符计数
const charCount = computed(() => {
  return displayText.value.length || 0
})

/**
 * 开始编辑模式
 */
function startEditing() {
  localText.value = props.modelValue
  isEditing.value = true
}

/**
 * 取消编辑
 */
function cancelEditing() {
  localText.value = ''
  isEditing.value = false
}

/**
 * 保存编辑
 */
function saveEditing() {
  emit('update:modelValue', localText.value)
  emit('save', localText.value)
  isEditing.value = false
  toast.success('已保存')
}

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
  if (isUploading.value) return
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
  // 不再实时同步到父组件，避免触发自动保存
  // 用户需要点击"保存"按钮才会同步
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

// 只读模式样式
.transcript-display {
  background: $color-bg-tertiary;
  border-radius: $radius-md;
  overflow: hidden;

  .display-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: $spacing-sm $spacing-md;
    background: $color-bg-secondary;
    border-bottom: 1px solid $color-bg-elevated;

    h4 {
      margin: 0;
      font-size: 0.875rem;
      font-weight: 600;
      color: $color-text-primary;
    }

    .edit-btn {
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 4px 10px;
      background: transparent;
      border: 1px solid $color-bg-elevated;
      border-radius: $radius-sm;
      color: $color-text-tertiary;
      font-size: 0.75rem;
      cursor: pointer;
      transition: all 0.2s;

      svg {
        opacity: 0.7;
      }

      &:hover {
        color: $color-accent;
        border-color: $color-accent;
        background: rgba($color-accent, 0.1);

        svg {
          opacity: 1;
        }
      }
    }
  }

  .transcript-content {
    padding: $spacing-md;
    max-height: 300px;
    overflow-y: auto;

    .transcript-text {
      margin: 0;
      font-size: 0.875rem;
      line-height: 1.7;
      color: $color-text-secondary;
      white-space: pre-wrap;
      word-break: break-word;
    }
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
  cursor: pointer;

  &:hover {
    border-color: $color-accent;
    background: rgba($color-accent, 0.05);
  }

  &.drag-over {
    border-color: $color-accent;
    background: rgba($color-accent, 0.1);
  }

  &.uploading {
    cursor: not-allowed;
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

  p {
    color: $color-text-secondary;
    font-size: 0.8125rem;
    margin: 0;
  }

  .format-hint {
    font-size: 0.6875rem;
    color: $color-text-tertiary;
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

// 编辑模式操作按钮
.edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: $spacing-sm;
  margin-top: $spacing-md;
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
