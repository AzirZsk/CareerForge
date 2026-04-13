<!--=====================================================
  CareerForge 首次使用引导页面
  @author Azir
=====================================================-->

<template>
  <div class="onboarding-page">
    <div class="onboarding-container">
      <!-- 欢迎区域 -->
      <div class="welcome-section">
        <div class="logo">
          <span class="logo-icon">🎯</span>
          <span class="logo-text">CareerForge</span>
        </div>
        <h1 class="welcome-title">
          欢迎使用 CareerForge
        </h1>
        <p class="welcome-subtitle">
          智能求职助手，助你拿下心仪的工作
        </p>
      </div>

      <!-- 上传简历 -->
      <div class="upload-section">
        <h2 class="section-title">
          上传你的简历
        </h2>
        <p class="section-desc">
          我们将通过AI解析简历，提取你的基本信息
        </p>

        <!-- 上传区域 -->
        <div
          class="upload-area"
          :class="{ 'drag-over': isDragOver, 'is-uploading': uploading }"
          @dragover.prevent="isDragOver = true"
          @dragleave.prevent="isDragOver = false"
          @drop.prevent="handleDrop"
          @click="triggerFileInput"
        >
          <input
            ref="fileInput"
            type="file"
            accept=".jpg,.jpeg,.png,.pdf"
            class="hidden-input"
            @change="handleFileSelect"
          >
          <div
            v-if="!uploading"
            class="upload-content"
          >
            <div class="upload-icon">
              <svg
                width="48"
                height="48"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.5"
              >
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <polyline points="17 8 12 3 7 8" />
                <line
                  x1="12"
                  y1="3"
                  x2="12"
                  y2="15"
                />
              </svg>
            </div>
            <p class="upload-text">
              点击或拖拽上传简历
            </p>
            <p class="upload-hint">
              支持 JPG、PNG 图片和 PDF 格式
            </p>
          </div>
          <div
            v-else
            class="upload-loading"
          >
            <div class="loading-spinner" />
            <p class="loading-title">
              正在解析简历
            </p>
            <p class="loading-desc">
              AI正在解析您的简历，请稍后...
            </p>
          </div>
        </div>

        <!-- 错误提示 -->
        <p
          v-if="errorMessage"
          class="error-message"
        >
          {{ errorMessage }}
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores'

const router = useRouter()
const store = useAppStore()

const isDragOver = ref<boolean>(false)
const uploading = ref<boolean>(false)
const fileInput = ref<HTMLInputElement | null>(null)
const errorMessage = ref<string>('')

function triggerFileInput(): void {
  if (!uploading.value) {
    fileInput.value?.click()
  }
}

function handleFileSelect(event: Event): void {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  // 重置 input value，确保选择同一文件时也能触发 change 事件
  target.value = ''
  if (file) {
    if (!validateFileType(file)) {
      errorMessage.value = '仅支持 JPG、PNG 图片和 PDF 格式'
      return
    }
    uploadResume(file)
  }
}

function handleDrop(event: DragEvent): void {
  isDragOver.value = false
  const file = event.dataTransfer?.files?.[0]
  if (file) {
    if (!validateFileType(file)) {
      errorMessage.value = '仅支持 JPG、PNG 图片和 PDF 格式'
      return
    }
    uploadResume(file)
  }
}

/**
 * 验证文件类型
 * 支持：jpg, jpeg, png, pdf
 */
function validateFileType(file: File): boolean {
  const allowedTypes = ['image/jpeg', 'image/png', 'application/pdf']
  const allowedExtensions = ['.jpg', '.jpeg', '.png', '.pdf']

  const fileTypeValid = allowedTypes.includes(file.type)
  const fileName = file.name.toLowerCase()
  const extensionValid = allowedExtensions.some(ext => fileName.endsWith(ext))

  return fileTypeValid || extensionValid
}

async function uploadResume(file: File): Promise<void> {
  uploading.value = true
  errorMessage.value = ''
  try {
    await store.initUser(file)
    router.push('/')
  } catch (error) {
    console.error('初始化失败', error)
    errorMessage.value = error instanceof Error ? error.message : '简历解析失败，请重试'
  } finally {
    uploading.value = false
  }
}
</script>

<style lang="scss" scoped>
.onboarding-page {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: $spacing-xl;
  overflow: hidden;
  background: $gradient-dark;
  z-index: 100;
}

.onboarding-container {
  width: 100%;
  max-width: 520px;
}

// 欢迎区域
.welcome-section {
  text-align: center;
  margin-bottom: $spacing-2xl;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-lg;
}

.logo-icon {
  font-size: 2.5rem;
}

.logo-text {
  font-family: $font-display;
  font-size: $text-3xl;
  font-weight: $weight-bold;
  color: $color-accent;
}

.welcome-title {
  font-family: $font-display;
  font-size: $text-4xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
  margin-bottom: $spacing-sm;
}

.welcome-subtitle {
  font-size: $text-base;
  color: $color-text-secondary;
}

// 上传区域
.upload-section {
  background: $gradient-card;
  border-radius: $radius-xl;
  padding: $spacing-2xl;
  border: 1px solid rgba(255, 255, 255, 0.05);
  text-align: center;
}

.section-title {
  font-family: $font-display;
  font-size: $text-2xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
  margin-bottom: $spacing-sm;
}

.section-desc {
  font-size: $text-sm;
  color: $color-text-secondary;
  margin-bottom: $spacing-xl;
}

.upload-area {
  padding: $spacing-2xl;
  border: 2px dashed rgba(255, 255, 255, 0.15);
  border-radius: $radius-lg;
  cursor: pointer;
  transition: all $transition-fast;
  &.drag-over,
  &:hover {
    border-color: $color-accent;
    background: rgba(212, 168, 83, 0.05);
  }
  &.is-uploading {
    cursor: not-allowed;
    pointer-events: none;
  }
}

.hidden-input {
  display: none;
}

.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-md;
}

.upload-icon {
  color: $color-accent;
}

.upload-text {
  font-size: $text-base;
  color: $color-text-primary;
}

.upload-hint {
  font-size: $text-sm;
  color: $color-text-tertiary;
}

.upload-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-md;
  padding: $spacing-lg 0;
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 3px solid rgba(255, 255, 255, 0.1);
  border-top-color: $color-accent;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: $spacing-sm;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.loading-title {
  font-size: $text-lg;
  font-weight: $weight-medium;
  color: $color-text-primary;
}

.loading-desc {
  font-size: $text-sm;
  color: $color-text-tertiary;
}

.error-message {
  margin-top: $spacing-md;
  font-size: $text-sm;
  color: $color-error;
}
</style>
