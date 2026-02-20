<!--=====================================================
  LandIt 首次使用引导页面
  @author Azir
=====================================================-->

<template>
  <div class="onboarding-page">
    <div class="onboarding-container">
      <!-- 欢迎区域 -->
      <div class="welcome-section">
        <div class="logo">
          <span class="logo-icon">🎯</span>
          <span class="logo-text">LandIt</span>
        </div>
        <h1 class="welcome-title">欢迎使用 LandIt</h1>
        <p class="welcome-subtitle">智能求职助手，助你拿下心仪的工作</p>
      </div>

      <!-- 引导步骤 -->
      <div class="steps-section">
        <!-- 步骤1: 上传简历 -->
        <div v-if="currentStep === 1" class="step-content animate-in">
          <h2 class="step-title">首先，让我们认识一下你</h2>
          <p class="step-desc">上传简历可以快速提取你的基本信息，或者手动填写</p>

          <!-- 上传区域 -->
          <div
            class="upload-area"
            :class="{ 'drag-over': isDragOver }"
            @dragover.prevent="isDragOver = true"
            @dragleave.prevent="isDragOver = false"
            @drop.prevent="handleDrop"
            @click="triggerFileInput"
          >
            <input
              ref="fileInput"
              type="file"
              accept=".pdf,.doc,.docx,.txt"
              class="hidden-input"
              @change="handleFileSelect"
            />
            <div v-if="!uploadingFile" class="upload-content">
              <div class="upload-icon">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                  <polyline points="17 8 12 3 7 8"></polyline>
                  <line x1="12" y1="3" x2="12" y2="15"></line>
                </svg>
              </div>
              <p class="upload-text">点击或拖拽上传简历</p>
              <p class="upload-hint">支持 PDF、Word、TXT 格式</p>
            </div>
            <div v-else class="upload-loading">
              <div class="loading-spinner"></div>
              <p class="loading-text">正在解析简历...</p>
            </div>
          </div>

          <!-- 跳过上传 -->
          <button class="skip-btn" @click="skipUpload">
            暂时跳过，手动填写
          </button>
        </div>

        <!-- 步骤2: 确认信息 -->
        <div v-if="currentStep === 2" class="step-content animate-in">
          <h2 class="step-title">确认你的信息</h2>
          <p class="step-desc">请确认或修改以下基本信息</p>

          <div class="form-section">
            <div class="form-group">
              <label class="form-label">姓名</label>
              <input
                v-model="userInfo.name"
                type="text"
                class="form-input"
                placeholder="请输入你的姓名"
              />
            </div>

            <div class="form-group">
              <label class="form-label">性别</label>
              <div class="gender-options">
                <button
                  class="gender-btn"
                  :class="{ active: userInfo.gender === 'MALE' }"
                  @click="userInfo.gender = 'MALE'"
                >
                  <span class="gender-icon">👨</span>
                  <span>男</span>
                </button>
                <button
                  class="gender-btn"
                  :class="{ active: userInfo.gender === 'FEMALE' }"
                  @click="userInfo.gender = 'FEMALE'"
                >
                  <span class="gender-icon">👩</span>
                  <span>女</span>
                </button>
              </div>
            </div>
          </div>

          <div class="action-buttons">
            <button class="back-btn" @click="currentStep = 1">
              返回
            </button>
            <button
              class="next-btn"
              :disabled="!userInfo.name"
              @click="completeOnboarding"
            >
              开始使用
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores'
import type { Gender } from '@/types'

const router = useRouter()
const store = useAppStore()

const currentStep = ref<number>(1)
const isDragOver = ref<boolean>(false)
const uploadingFile = ref<boolean>(false)
const fileInput = ref<HTMLInputElement | null>(null)

const userInfo = reactive<{
  name: string
  gender: Gender | null
}>({
  name: '',
  gender: null
})

function triggerFileInput(): void {
  fileInput.value?.click()
}

function handleFileSelect(event: Event): void {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {
    parseResumeFile(file)
  }
}

function handleDrop(event: DragEvent): void {
  isDragOver.value = false
  const file = event.dataTransfer?.files?.[0]
  if (file) {
    parseResumeFile(file)
  }
}

async function parseResumeFile(file: File): Promise<void> {
  uploadingFile.value = true
  try {
    const result = await store.parseResume(file)
    if (result.name) {
      userInfo.name = result.name
    }
    if (result.gender) {
      userInfo.gender = result.gender
    }
    currentStep.value = 2
  } catch (error) {
    console.error('解析简历失败', error)
    currentStep.value = 2
  } finally {
    uploadingFile.value = false
  }
}

function skipUpload(): void {
  currentStep.value = 2
}

async function completeOnboarding(): Promise<void> {
  if (!userInfo.name) return
  try {
    await store.initUser({
      name: userInfo.name,
      gender: userInfo.gender
    })
    router.push('/')
  } catch (error) {
    console.error('初始化失败', error)
  }
}
</script>

<style lang="scss" scoped>
.onboarding-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: $spacing-xl;
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

// 步骤区域
.steps-section {
  background: $gradient-card;
  border-radius: $radius-xl;
  padding: $spacing-2xl;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.step-content {
  text-align: center;
}

.step-title {
  font-family: $font-display;
  font-size: $text-2xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
  margin-bottom: $spacing-sm;
}

.step-desc {
  font-size: $text-sm;
  color: $color-text-secondary;
  margin-bottom: $spacing-xl;
}

// 上传区域
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
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(255, 255, 255, 0.1);
  border-top-color: $color-accent;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.loading-text {
  font-size: $text-sm;
  color: $color-text-secondary;
}

.skip-btn {
  margin-top: $spacing-lg;
  padding: $spacing-sm $spacing-lg;
  background: transparent;
  color: $color-text-tertiary;
  font-size: $text-sm;
  border-radius: $radius-md;
  transition: color $transition-fast;
  &:hover {
    color: $color-text-secondary;
  }
}

// 表单
.form-section {
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
  text-align: left;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.form-label {
  font-size: $text-sm;
  color: $color-text-tertiary;
  font-weight: $weight-medium;
}

.form-input {
  padding: $spacing-md;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-md;
  color: $color-text-primary;
  font-size: $text-base;
  transition: all $transition-fast;
  &:focus {
    outline: none;
    border-color: $color-accent;
  }
}

.gender-options {
  display: flex;
  gap: $spacing-md;
}

.gender-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-lg;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-md;
  color: $color-text-secondary;
  transition: all $transition-fast;
  &.active {
    background: $color-accent-glow;
    border-color: $color-accent;
    color: $color-accent;
  }
  &:hover:not(.active) {
    background: rgba(255, 255, 255, 0.05);
  }
}

.gender-icon {
  font-size: 1.5rem;
}

// 操作按钮
.action-buttons {
  display: flex;
  gap: $spacing-md;
  margin-top: $spacing-2xl;
}

.back-btn {
  flex: 1;
  padding: $spacing-md $spacing-lg;
  background: rgba(255, 255, 255, 0.05);
  color: $color-text-secondary;
  font-size: $text-base;
  border-radius: $radius-md;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all $transition-fast;
  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: $color-text-primary;
  }
}

.next-btn {
  flex: 2;
  padding: $spacing-md $spacing-lg;
  background: $gradient-gold;
  color: $color-bg-deep;
  font-size: $text-base;
  font-weight: $weight-medium;
  border-radius: $radius-md;
  transition: all $transition-fast;
  &:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(212, 168, 83, 0.3);
  }
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

// 动画
.animate-in {
  animation: slideUp 0.5s ease forwards;
}
</style>
