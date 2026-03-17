<!--=====================================================
  通用确认弹窗组件
  @author Azir
=====================================================-->

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="visible"
        class="modal-overlay"
        :class="{ 'modal-overlay--high': overlay }"
        @click.self="handleCancel"
      >
        <div class="modal-container">
          <header class="modal-header">
            <h3 class="modal-title">{{ title }}</h3>
            <button class="close-btn" @click="handleCancel">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </button>
          </header>

          <div class="modal-body">
            <p class="confirm-message">{{ message }}</p>
          </div>

          <footer class="modal-footer">
            <button class="btn-cancel" @click="handleCancel">取消</button>
            <button class="btn-confirm" :class="{ danger: danger }" @click="handleConfirm">
              {{ confirmText }}
            </button>
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  visible: boolean
  title?: string
  message: string
  confirmText?: string
  danger?: boolean
  /** 是否显示在全屏遮罩层之上 */
  overlay?: boolean
}>(), {
  title: '确认',
  confirmText: '确定',
  danger: false,
  overlay: false
})

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'confirm': []
  'cancel': []
}>()

function handleConfirm(): void {
  emit('confirm')
  emit('update:visible', false)
}

function handleCancel(): void {
  emit('cancel')
  emit('update:visible', false)
}
</script>

<style lang="scss" scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: $z-modal;

  // 高层级模式：显示在全屏遮罩层之上
  &.modal-overlay--high {
    z-index: $z-modal-overlay;
  }
}

.modal-container {
  background: $color-bg-secondary;
  border-radius: $radius-lg;
  border: 1px solid rgba(255, 255, 255, 0.08);
  width: 90%;
  max-width: 400px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-lg;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.modal-title {
  font-family: $font-display;
  font-size: $text-xl;
  font-weight: $weight-semibold;
  color: $color-text-primary;
}

.close-btn {
  color: $color-text-tertiary;
  transition: color $transition-fast;
  &:hover {
    color: $color-text-primary;
  }
}

.modal-body {
  padding: $spacing-lg;
}

.confirm-message {
  font-size: $text-base;
  color: $color-text-secondary;
  line-height: $leading-relaxed;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: $spacing-md;
  padding: $spacing-lg;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.btn-cancel {
  padding: $spacing-sm $spacing-lg;
  font-size: $text-sm;
  color: $color-text-secondary;
  background: rgba(255, 255, 255, 0.05);
  border-radius: $radius-sm;
  transition: all $transition-fast;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: $color-text-primary;
  }
}

.btn-confirm {
  padding: $spacing-sm $spacing-lg;
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-text-primary;
  background: $color-accent;
  border-radius: $radius-sm;
  transition: all $transition-fast;

  &:hover {
    background: $color-accent-light;
  }

  &.danger {
    background: $color-error;

    &:hover {
      background: color-mix(in srgb, $color-error 80%, white);
    }
  }
}

// 动画
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.25s ease;

  .modal-container {
    transition: transform 0.25s ease, opacity 0.25s ease;
  }
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;

  .modal-container {
    transform: scale(0.95) translateY(-10px);
    opacity: 0;
  }
}
</style>
