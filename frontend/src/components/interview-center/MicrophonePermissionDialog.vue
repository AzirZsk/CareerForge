<!--=====================================================
  麦克风权限引导弹窗
  处理麦克风权限检查、请求、引导设置
  @author Azir
=====================================================-->

<template>
  <Teleport to="body">
    <div class="permission-overlay" @click.self="handleCancel">
      <div class="permission-dialog">
        <!-- 头部 -->
        <header class="dialog-header">
          <h2>麦克风权限</h2>
          <button class="close-btn" @click="handleCancel" v-if="canClose">×</button>
        </header>

        <!-- 内容区 -->
        <div class="dialog-content">
          <!-- 检查中/请求中状态 -->
          <div v-if="state === 'checking' || state === 'requesting'" class="state-content">
            <div class="icon-wrapper requesting">
              <svg class="mic-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
                <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
                <line x1="12" y1="19" x2="12" y2="23"/>
                <line x1="8" y1="23" x2="16" y2="23"/>
              </svg>
              <div class="pulse-ring"></div>
            </div>
            <p class="state-text">{{ state === 'checking' ? '正在检查麦克风权限...' : '请在浏览器弹窗中允许访问麦克风' }}</p>
            <p class="hint-text">面试需要使用麦克风录制您的语音回答</p>
          </div>

          <!-- 成功状态 -->
          <div v-else-if="state === 'success'" class="state-content">
            <div class="icon-wrapper success">
              <svg class="check-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
            </div>
            <p class="state-text">麦克风已授权</p>
            <p class="hint-text">正在进入面试...</p>
          </div>

          <!-- 拒绝状态（临时拒绝） -->
          <div v-else-if="state === 'denied'" class="state-content">
            <div class="icon-wrapper denied">
              <svg class="mic-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
                <line x1="1" y1="1" x2="23" y2="23"/>
              </svg>
            </div>
            <p class="state-text">麦克风权限被拒绝</p>
            <p class="hint-text">面试需要使用麦克风，请允许访问后重试</p>
          </div>

          <!-- 永久拒绝状态（需要手动设置） -->
          <div v-else-if="state === 'permanently_denied'" class="state-content">
            <div class="icon-wrapper permanently-denied">
              <svg class="settings-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="3"/>
                <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
              </svg>
            </div>
            <p class="state-text">需要在浏览器中手动设置</p>

            <!-- 浏览器设置步骤 -->
            <div class="settings-guide">
              <p class="browser-name">{{ browserGuide.browser }}</p>
              <ol class="steps-list">
                <li v-for="(step, index) in browserGuide.steps" :key="index">{{ step }}</li>
              </ol>
            </div>
          </div>

          <!-- 设备错误 -->
          <div v-else-if="state === 'device_error'" class="state-content">
            <div class="icon-wrapper error">
              <svg class="error-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <line x1="15" y1="9" x2="9" y2="15"/>
                <line x1="9" y1="9" x2="15" y2="15"/>
              </svg>
            </div>
            <p class="state-text">麦克风不可用</p>
            <p class="hint-text">{{ errorMessage || '请检查麦克风设备是否正常连接' }}</p>
          </div>
        </div>

        <!-- 底部按钮 -->
        <footer class="dialog-footer">
          <button
            v-if="state === 'checking' || state === 'requesting'"
            class="btn btn-secondary"
            @click="handleCancel"
          >
            取消
          </button>
          <button
            v-if="state === 'denied'"
            class="btn btn-primary"
            @click="handleRetry"
          >
            重试
          </button>
          <button
            v-if="state === 'permanently_denied'"
            class="btn btn-secondary"
            @click="handleCancel"
          >
            取消
          </button>
          <button
            v-if="state === 'permanently_denied'"
            class="btn btn-primary"
            @click="handleRetryAfterSettings"
          >
            我已设置，重新检查
          </button>
          <button
            v-if="state === 'device_error'"
            class="btn btn-secondary"
            @click="handleCancel"
          >
            取消
          </button>
          <button
            v-if="state === 'device_error'"
            class="btn btn-primary"
            @click="handleRetry"
          >
            重试
          </button>
        </footer>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { useScrollLock } from '@vueuse/core'
import { useMicrophonePermission } from '@/composables/useMicrophonePermission'

export type PermissionDialogState = 'checking' | 'requesting' | 'success' | 'denied' | 'permanently_denied' | 'device_error'

const props = defineProps<{
  state: PermissionDialogState
  errorMessage?: string
}>()

const emit = defineEmits<{
  close: []
  retry: []
  retryAfterSettings: []
}>()

const isScrollLocked = useScrollLock(document.body)
const { getBrowserSettingsGuide } = useMicrophonePermission()

const canClose = computed(() => {
  return props.state !== 'requesting'
})

const browserGuide = computed(() => getBrowserSettingsGuide())

function handleCancel() {
  if (props.state !== 'requesting') {
    emit('close')
  }
}

function handleRetry() {
  emit('retry')
}

function handleRetryAfterSettings() {
  emit('retryAfterSettings')
}

onMounted(() => {
  isScrollLocked.value = true
})

onUnmounted(() => {
  isScrollLocked.value = false
})
</script>

<style scoped lang="scss">
.permission-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: $z-modal-overlay;
}

.permission-dialog {
  background: $color-bg-secondary;
  border-radius: $radius-xl;
  width: 90%;
  max-width: 440px;
  padding: $spacing-xl;
  text-align: center;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-xl;

  h2 {
    font-family: $font-display;
    font-size: $text-xl;
    font-weight: $weight-semibold;
    color: $color-text-primary;
  }

  .close-btn {
    background: none;
    border: none;
    color: $color-text-tertiary;
    font-size: 1.5rem;
    cursor: pointer;
    line-height: 1;
    transition: color 0.2s;

    &:hover {
      color: $color-text-primary;
    }
  }
}

.dialog-content {
  margin-bottom: $spacing-xl;
}

.state-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-md;
}

.icon-wrapper {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;

  &.requesting {
    background: rgba($color-accent, 0.15);
    color: $color-accent;

    .pulse-ring {
      position: absolute;
      inset: 0;
      border-radius: 50%;
      border: 2px solid $color-accent;
      animation: pulse 1.5s ease-out infinite;
    }
  }

  &.success {
    background: rgba($color-success, 0.15);
    color: $color-success;
  }

  &.denied {
    background: rgba($color-warning, 0.15);
    color: $color-warning;
  }

  &.permanently-denied {
    background: rgba($color-error, 0.15);
    color: $color-error;
  }

  &.error {
    background: rgba($color-error, 0.15);
    color: $color-error;
  }

  svg {
    width: 36px;
    height: 36px;
  }
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  100% {
    transform: scale(1.5);
    opacity: 0;
  }
}

.state-text {
  font-size: $text-lg;
  font-weight: $weight-medium;
  color: $color-text-primary;
}

.hint-text {
  font-size: $text-sm;
  color: $color-text-tertiary;
}

.settings-guide {
  margin-top: $spacing-md;
  padding: $spacing-md;
  background: $color-bg-tertiary;
  border-radius: $radius-md;
  text-align: left;
  width: 100%;

  .browser-name {
    font-size: $text-sm;
    font-weight: $weight-medium;
    color: $color-accent;
    margin-bottom: $spacing-sm;
  }

  .steps-list {
    padding-left: $spacing-lg;
    margin: 0;

    li {
      font-size: $text-sm;
      color: $color-text-secondary;
      margin-bottom: $spacing-xs;
      line-height: 1.5;

      &:last-child {
        margin-bottom: 0;
      }
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: center;
  gap: $spacing-md;
}

.btn {
  padding: $spacing-sm $spacing-xl;
  font-size: $text-sm;
  font-weight: $weight-medium;
  border-radius: $radius-md;
  transition: all $transition-fast;
  cursor: pointer;
  border: none;
}

.btn-secondary {
  background: rgba(255, 255, 255, 0.05);
  color: $color-text-secondary;
  border: 1px solid rgba(255, 255, 255, 0.1);

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: $color-text-primary;
  }
}

.btn-primary {
  background: $gradient-gold;
  color: $color-bg-deep;

  &:hover {
    transform: translateY(-1px);
    opacity: 0.9;
  }
}
</style>
