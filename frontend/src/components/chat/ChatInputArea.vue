<!--=====================================================
  聊天输入区域组件
  支持文本输入、图片上传、发送按钮
  @author Azir
=====================================================-->

<template>
  <div class="chat-input-area">
    <!-- 图片预览 -->
    <Transition name="slide-up">
      <div v-if="selectedImage" class="image-preview">
        <img :src="imageUrl" alt="待发送图片" />
        <button class="remove-btn" @click="handleImageRemove">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"></line>
            <line x1="6" y1="6" x2="18" y2="18"></line>
          </svg>
        </button>
      </div>
    </Transition>

    <!-- 输入区域 -->
    <div class="input-container">
      <!-- 图片上传按钮 -->
      <button
        class="upload-btn"
        @click="triggerFileInput"
        :disabled="isStreaming"
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
        @change="handleFileSelect"
        hidden
      />

      <!-- 文本输入框 -->
      <textarea
        v-model="inputText"
        @keydown="handleKeydown"
        placeholder="输入消息，按Enter发送..."
        :disabled="isStreaming"
        rows="1"
        class="message-input"
      ></textarea>

      <!-- 发送按钮 -->
      <button
        class="send-btn"
        @click="handleSend"
        :disabled="isStreaming || (!inputText.trim() && !selectedImage)"
        :class="{ active: inputText.trim() || selectedImage }"
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

interface Props {
  modelValue: string
  selectedImage: File | null
  isStreaming: boolean
}

interface Emits {
  (e: 'update:modelValue', value: string): void
  (e: 'send'): void
  (e: 'imageSelect', file: File): void
  (e: 'imageRemove'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const fileInputRef = ref<HTMLInputElement | null>(null)
const inputText = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 图片预览URL
const imageUrl = computed(() => {
  if (props.selectedImage) {
    return URL.createObjectURL(props.selectedImage)
  }
  return undefined
})

// 清理图片URL
watch(imageUrl, (_newUrl, oldUrl) => {
  if (oldUrl) {
    URL.revokeObjectURL(oldUrl)
  }
})

function triggerFileInput() {
  fileInputRef.value?.click()
}

function handleFileSelect(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {
    emit('imageSelect', file)
  }
  // 重置input以便可以重复选择同一文件
  target.value = ''
}

function handleImageRemove() {
  emit('imageRemove')
}

function handleSend() {
  if (!props.isStreaming && (inputText.value.trim() || props.selectedImage)) {
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
</script>

<style lang="scss" scoped>
.chat-input-area {
  padding: $spacing-md $spacing-lg;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  background: $color-bg-secondary;
}

.image-preview {
  position: relative;
  display: inline-block;
  margin-bottom: $spacing-sm;
  border-radius: $radius-sm;
  overflow: hidden;

  img {
    max-height: 100px;
    border-radius: $radius-sm;
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

    &:hover {
      background: rgba($color-error, 0.8);
    }
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
