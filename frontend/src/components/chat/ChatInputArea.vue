<!--=====================================================
  聊天输入区域组件
  支持文本输入、多图上传、发送按钮
  @author Azir
=====================================================-->

<template>
  <div class="chat-input-area">
    <!-- 多图预览列表 -->
    <TransitionGroup name="slide-up" tag="div" class="image-preview-list" v-if="selectedImages.length > 0">
      <div
        v-for="(_image, index) in selectedImages"
        :key="index"
        class="image-preview-item"
      >
        <div class="image-preview" @click="handleImagePreview(index)">
          <img :src="getImageUrl(index)" alt="待发送图片" />
          <div class="image-overlay">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              <line x1="11" y1="8" x2="11" y2="14"></line>
              <line x1="8" y1="11" x2="14" y2="11"></line>
            </svg>
          </div>
          <button class="remove-btn" @click.stop="handleImageRemove(index)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
        <div class="image-index">{{ index + 1 }}/{{ maxImageCount }}</div>
      </div>

      <!-- 添加更多图片按钮 -->
      <div
        v-if="selectedImages.length < maxImageCount"
        key="add-more"
        class="add-more-btn"
        @click="triggerFileInput"
      >
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="12" y1="5" x2="12" y2="19"></line>
          <line x1="5" y1="12" x2="19" y2="12"></line>
        </svg>
      </div>
    </TransitionGroup>

    <!-- 输入区域 -->
    <div class="input-container">
      <!-- 图片上传按钮 -->
      <button
        class="upload-btn"
        @click="triggerFileInput"
        :disabled="isStreaming || selectedImages.length >= maxImageCount"
        title="上传图片"
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
          <circle cx="8.5" cy="8.5" r="1.5"></circle>
          <polyline points="21 15 16 10 5 21"></polyline>
        </svg>
      </button>
      <input
        ref="fileInputRef"
        type="file"
        accept="image/*"
        multiple
        @change="handleFileSelect"
        hidden
      />

      <!-- 文本输入框 -->
      <textarea
        v-model="inputText"
        @keydown="handleKeydown"
        @paste="handlePaste"
        placeholder="输入消息，按Enter发送，Ctrl+V粘贴图片..."
        :disabled="isStreaming"
        rows="1"
        class="message-input"
      ></textarea>

      <!-- 发送按钮 -->
      <button
        class="send-btn"
        @click="handleSend"
        :disabled="isStreaming || (!inputText.trim() && selectedImages.length === 0)"
        :class="{ active: inputText.trim() || selectedImages.length > 0 }"
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="22" y1="2" x2="11" y2="13"></line>
          <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { MAX_IMAGE_COUNT } from '@/types/ai-chat'

interface Props {
  modelValue: string
  selectedImages: File[]
  isStreaming: boolean
}

interface Emits {
  (e: 'update:modelValue', value: string): void
  (e: 'send'): void
  (e: 'imageSelect', file: File): void
  (e: 'imageRemove', index: number): void
  (e: 'previewImage', url: string): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const maxImageCount = MAX_IMAGE_COUNT
const fileInputRef = ref<HTMLInputElement | null>(null)
const inputText = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 图片URL缓存
const imageUrlCache = ref<Map<number, string>>(new Map())

// 获取图片URL（带缓存）
function getImageUrl(index: number): string {
  if (!imageUrlCache.value.has(index)) {
    imageUrlCache.value.set(index, URL.createObjectURL(props.selectedImages[index]))
  }
  return imageUrlCache.value.get(index)!
}

// 清理不再存在的图片URL
watch(() => props.selectedImages, (newImages, oldImages) => {
  const oldLength = oldImages?.length || 0
  for (let i = newImages.length; i < oldLength; i++) {
    const url = imageUrlCache.value.get(i)
    if (url) {
      URL.revokeObjectURL(url)
      imageUrlCache.value.delete(i)
    }
  }
}, { deep: true })

function triggerFileInput() {
  if (props.selectedImages.length >= maxImageCount) return
  fileInputRef.value?.click()
}

function handleFileSelect(event: Event) {
  const target = event.target as HTMLInputElement
  const files = target.files
  if (!files) return

  // 检查剩余可添加数量
  const remaining = maxImageCount - props.selectedImages.length
  if (remaining <= 0) {
    target.value = ''
    return
  }

  // 只取允许的数量
  const filesToAdd = Array.from(files).slice(0, remaining)
  filesToAdd.forEach(file => emit('imageSelect', file))

  target.value = ''
}

function handleImageRemove(index: number) {
  emit('imageRemove', index)
}

function handleImagePreview(index: number) {
  const url = getImageUrl(index)
  if (url) {
    emit('previewImage', url)
  }
}

function handleSend() {
  if (!props.isStreaming && (inputText.value.trim() || props.selectedImages.length > 0)) {
    emit('send')
  }
}

function handleKeydown(event: KeyboardEvent) {
  // Enter发送，Shift+Enter换行
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    handleSend()
  }
}

/**
 * 处理粘贴事件，支持从剪贴板粘贴图片
 */
function handlePaste(event: ClipboardEvent): void {
  // 如果正在流式传输或已达上限，不处理粘贴
  if (props.isStreaming) return
  if (props.selectedImages.length >= maxImageCount) return

  const clipboardData = event.clipboardData
  if (!clipboardData) return

  // 检查剪贴板项中是否有图片
  const items = clipboardData.items
  for (let i = 0; i < items.length; i++) {
    const item = items[i]
    // 检查是否为图片类型
    if (item.type.startsWith('image/')) {
      const file = item.getAsFile()
      if (file) {
        // 复用现有的图片选择逻辑
        emit('imageSelect', file)
        // 阻止默认粘贴行为（避免图片作为文本粘贴）
        event.preventDefault()
        break
      }
    }
  }
  // 如果没有图片，保持默认的文本粘贴行为
}
</script>

<style lang="scss" scoped>
.chat-input-area {
  padding: $spacing-md $spacing-xl $spacing-xl;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  background: $color-bg-secondary;
  width: 100%;
}

.image-preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm;
  margin-bottom: $spacing-sm;
}

.image-preview-item {
  position: relative;
}

.image-preview {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: $radius-sm;
  overflow: hidden;
  cursor: pointer;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform $transition-fast;
  }

  .image-overlay {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.4);
    display: flex;
    align-items: center;
    justify-content: center;
    opacity: 0;
    transition: opacity $transition-fast;
    color: $color-text-primary;
  }

  &:hover {
    img {
      transform: scale(1.05);
    }

    .image-overlay {
      opacity: 1;
    }
  }

  .remove-btn {
    position: absolute;
    top: 4px;
    right: 4px;
    width: 20px;
    height: 20px;
    background: rgba(0, 0, 0, 0.6);
    border-radius: $radius-full;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    cursor: pointer;
    transition: all 0.2s ease;
    z-index: 1;

    &:hover {
      background: rgba($color-error, 0.8);
    }
  }
}

.image-index {
  position: absolute;
  bottom: 4px;
  left: 4px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: $radius-sm;
}

.add-more-btn {
  width: 80px;
  height: 80px;
  border: 2px dashed rgba(255, 255, 255, 0.2);
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $color-text-tertiary;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: rgba($color-accent, 0.5);
    color: $color-accent;
  }
}

.input-container {
  display: flex;
  align-items: flex-end;
  gap: $spacing-sm;
}

.upload-btn,
.send-btn {
  width: 40px;
  height: 40px;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $color-text-tertiary;
  background: $color-bg-tertiary;
  border: 1px solid rgba(255, 255, 255, 0.06);
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;

  &:hover:not(:disabled) {
    color: $color-text-primary;
    border-color: rgba(255, 255, 255, 0.1);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.send-btn.active {
  background: $color-accent;
  color: white;
  border-color: $color-accent;

  &:hover:not(:disabled) {
    background: $color-accent-light;
  }
}

.message-input {
  flex: 1;
  padding: $spacing-sm $spacing-md;
  background: $color-bg-tertiary;
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: $radius-md;
  color: $color-text-primary;
  font-family: $font-body;
  font-size: $text-base;
  line-height: 1.5;
  resize: none;
  max-height: 120px;

  &::placeholder {
    color: $color-text-tertiary;
  }

  &:focus {
    outline: none;
    border-color: rgba($color-accent, 0.3);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.1);
    border-radius: 2px;
  }
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.2s ease;
}

.slide-up-enter-from,
.slide-up-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
