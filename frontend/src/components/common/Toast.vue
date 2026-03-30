<!--=====================================================
  Toast 通知组件
  @author Azir
=====================================================-->

<template>
  <Teleport to="body">
    <div class="toast-container">
      <TransitionGroup name="toast">
        <div
          v-for="toast in toasts"
          :key="toast.id"
          :class="['toast', `toast-${toast.type}`]"
        >
          <div
            class="toast-icon"
            v-html="getIcon(toast.type)"
          />
          <span class="toast-message">{{ toast.message }}</span>
          <button
            class="toast-close"
            @click="removeToast(toast.id)"
          >
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <line
                x1="18"
                y1="6"
                x2="6"
                y2="18"
              />
              <line
                x1="6"
                y1="6"
                x2="18"
                y2="18"
              />
            </svg>
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref } from 'vue'

export type ToastType = 'success' | 'error' | 'warning' | 'info'

export interface ToastItem {
  id: number
  type: ToastType
  message: string
  duration: number
}

const toasts = ref<ToastItem[]>([])
let toastId = 0

// 图标 SVG 映射
const icons: Record<ToastType, string> = {
  success: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"></polyline></svg>',
  error: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line></svg>',
  warning: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>',
  info: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>'
}

function getIcon(type: ToastType): string {
  return icons[type]
}

function addToast(type: ToastType, message: string, duration = 3000): number {
  const id = ++toastId
  toasts.value.push({ id, type, message, duration })

  if (duration > 0) {
    setTimeout(() => removeToast(id), duration)
  }

  return id
}

function removeToast(id: number): void {
  const index = toasts.value.findIndex(t => t.id === id)
  if (index !== -1) {
    toasts.value.splice(index, 1)
  }
}

function success(message: string, duration?: number): number {
  return addToast('success', message, duration)
}

function error(message: string, duration?: number): number {
  return addToast('error', message, duration)
}

function warning(message: string, duration?: number): number {
  return addToast('warning', message, duration)
}

function info(message: string, duration?: number): number {
  return addToast('info', message, duration)
}

defineExpose({
  success,
  error,
  warning,
  info,
  addToast,
  removeToast
})
</script>

<style lang="scss" scoped>
.toast-container {
  position: fixed;
  top: $spacing-lg;
  right: $spacing-lg;
  z-index: $z-toast;
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
  pointer-events: none;
}

.toast {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-md $spacing-lg;
  background: $color-bg-secondary;
  border-radius: $radius-md;
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: $shadow-lg;
  pointer-events: auto;
  min-width: 280px;
  max-width: 400px;
}

.toast-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.toast-message {
  flex: 1;
  font-size: $text-sm;
  color: $color-text-primary;
  line-height: $leading-normal;
}

.toast-close {
  flex-shrink: 0;
  color: $color-text-tertiary;
  transition: color $transition-fast;
  padding: $spacing-xs;

  &:hover {
    color: $color-text-primary;
  }
}

// Toast types
.toast-success {
  border-color: rgba($color-success, 0.3);

  .toast-icon {
    color: $color-success;
  }
}

.toast-error {
  border-color: rgba($color-error, 0.3);

  .toast-icon {
    color: $color-error;
  }
}

.toast-warning {
  border-color: rgba($color-warning, 0.3);

  .toast-icon {
    color: $color-warning;
  }
}

.toast-info {
  border-color: rgba($color-info, 0.3);

  .toast-icon {
    color: $color-info;
  }
}

// Animations
.toast-enter-active {
  transition: all 0.3s ease;
}

.toast-leave-active {
  transition: all 0.2s ease;
}

.toast-enter-from {
  opacity: 0;
  transform: translateX(100%);
}

.toast-leave-to {
  opacity: 0;
  transform: translateX(100%);
}
</style>
