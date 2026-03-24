<!--=====================================================
  AI聊天悬浮按钮组件
  右下角悬浮按钮，点击弹出聊天窗口
  @author Azir
=====================================================-->

<template>
  <div class="ai-chat-float">
    <!-- 悬浮按钮 -->
    <Transition name="fade">
      <button
        v-if="!state.isWindowOpen"
        class="float-button"
        @click="toggleWindow"
        title="AI简历助手"
      >
        <!-- AI图标 -->
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 2a2 2 0 0 1 2 2c0 .74-.4 1.39-1 1.73V7h1a7 7 0 0 1 7 7h1a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1h-1v1a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-1H2a1 1 0 0 1-1-1v-3a1 1 0 0 1 1-1h1a7 7 0 0 1 7-7h1V5.73c-.6-.34-1-.99-1-1.73a2 2 0 0 1 2-2z"/>
          <circle cx="8" cy="14" r="1.5" fill="currentColor"/>
          <circle cx="16" cy="14" r="1.5" fill="currentColor"/>
          <path d="M9 18c.83.67 1.83 1 3 1s2.17-.33 3-1"/>
        </svg>

        <!-- 提示文字 -->
        <span class="button-text">AI助手</span>
      </button>
    </Transition>

    <!-- 聊天窗口 -->
    <AIChatWindow
      v-model:open="state.isWindowOpen"
      @close="closeWindow"
    />
  </div>
</template>

<script setup lang="ts">
import { useAIChat } from '@/composables/useAIChat'
import AIChatWindow from './AIChatWindow.vue'

const { state, toggleWindow, closeWindow } = useAIChat()
</script>

<style lang="scss" scoped>
.ai-chat-float {
  position: fixed;
  bottom: $spacing-xl;
  right: $spacing-xl;
  z-index: 900;
}

.float-button {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-md $spacing-lg;
  border-radius: $radius-full;
  background: linear-gradient(135deg, $color-accent, $color-accent-dark);
  box-shadow: 0 4px 20px rgba(212, 168, 83, 0.4);
  cursor: pointer;
  color: white;
  border: none;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 30px rgba(212, 168, 83, 0.5);
  }

  &:active {
    transform: translateY(0);
  }

  svg {
    flex-shrink: 0;
  }

  .button-text {
    font-family: $font-body;
    font-size: $text-sm;
    font-weight: $weight-medium;
    white-space: nowrap;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: scale(0.9);
}
</style>
