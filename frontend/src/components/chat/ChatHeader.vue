<!--=====================================================
  聊天窗口头部组件
  显示AI识别到的简历信息
  支持新会话和关闭操作
  @author Azir
=====================================================-->

<template>
  <header class="chat-header">
    <div class="header-left">
      <!-- AI图标和标题 -->
      <div class="header-title">
        <AIIcon :size="28" />
        <span class="title">求职助手</span>
        <!-- 显示AI识别到的简历 -->
        <span
          v-if="detectedResume"
          class="resume-badge"
          @click="showSwitchTip"
        >
          <svg
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
            <polyline points="14 2 14 8 20 8" />
          </svg>
          {{ detectedResume.name }}
        </span>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="header-actions">
      <!-- 新会话按钮 -->
      <button
        class="new-session-btn"
        @click="$emit('newSession')"
      >
        新会话
      </button>

      <!-- 关闭按钮 -->
      <button
        class="close-btn"
        @click="$emit('close')"
      >
        <svg
          width="20"
          height="20"
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

    <!-- 切换简历提示 -->
    <Transition name="fade">
      <div
        v-if="showTip"
        class="switch-tip"
      >
        <span>在对话中告诉我想操作哪份简历，例如："帮我优化腾讯那份简历"</span>
        <button
          class="tip-close"
          @click="showTip = false"
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
    </Transition>
  </header>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AIIcon from '@/components/common/AIIcon.vue'

interface Props {
  detectedResume: {
    id: string
    name: string
  } | null
}

interface Emits {
  (e: 'close'): void
  (e: 'newSession'): void
}

defineProps<Props>()
defineEmits<Emits>()

const showTip = ref(false)

function showSwitchTip() {
  showTip.value = true
  // 3秒后自动关闭
  setTimeout(() => {
    showTip.value = false
  }, 3000)
}
</script>

<style lang="scss" scoped>
.chat-header {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-lg $spacing-xl;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  background: $color-bg-tertiary;
}

.header-left {
  display: flex;
  align-items: center;
  gap: $spacing-xl;
}

.header-title {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  color: $color-accent;

  svg {
    width: 28px;
    height: 28px;
  }

  .title {
    font-family: $font-display;
    font-size: $text-xl;
    font-weight: $weight-semibold;
    color: $color-text-primary;
  }
}

.resume-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  background: rgba($color-accent, 0.15);
  border: 1px solid rgba($color-accent, 0.3);
  border-radius: $radius-full;
  font-size: $text-sm;
  font-weight: $weight-medium;
  color: $color-accent;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: rgba($color-accent, 0.25);
    border-color: rgba($color-accent, 0.5);
  }

  svg {
    width: 14px;
    height: 14px;
    opacity: 0.8;
  }
}

.close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  color: $color-text-tertiary;
  padding: $spacing-sm;
  border-radius: $radius-md;
  transition: all 0.2s ease;

  &:hover {
    color: $color-text-primary;
    background: rgba(255, 255, 255, 0.08);
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
}

.new-session-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: $spacing-sm $spacing-md;
  color: $color-text-secondary;
  font-size: $text-sm;
  font-weight: $weight-medium;
  border-radius: $radius-md;
  transition: all 0.2s ease;
  white-space: nowrap;

  &:hover {
    color: $color-accent;
    background: rgba(212, 168, 83, 0.1);
  }
}

.switch-tip {
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: $spacing-md;
  padding: $spacing-md $spacing-lg;
  background: $color-bg-elevated;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: $radius-md;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  font-size: $text-sm;
  color: $color-text-secondary;
  z-index: 10;
  white-space: nowrap;

  .tip-close {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 4px;
    color: $color-text-tertiary;
    border-radius: $radius-sm;
    transition: all 0.2s ease;

    &:hover {
      color: $color-text-primary;
      background: rgba(255, 255, 255, 0.1);
    }
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-8px);
}
</style>
