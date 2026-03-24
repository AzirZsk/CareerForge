<!--=====================================================
  AI聊天悬浮按钮组件
  右下角悬浮按钮，点击弹出聊天窗口
  @author Azir
=====================================================-->

<template>
  <div class="ai-chat-float">
    <!-- 悬浮按钮 -->
    <Transition name="float-fade">
      <button
        v-if="!state.isWindowOpen"
        class="float-button"
        @click="toggleWindow"
        title="AI简历助手"
      >
        <!-- AI图标 -->
        <svg class="ai-icon" width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 2a2 2 0 0 1 2 2c0 .74-.4 1.39-1 1.73V7h1a7 7 0 0 1 7 7h1a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1h-1v1a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-1H2a1 1 0 0 1-1-1v-3a1 1 0 0 1 1-1h1a7 7 0 0 1 7-7h1V5.73c-.6-.34-1-.99-1-1.73a2 2 0 0 1 2-2z"/>
          <circle cx="8" cy="14" r="1.5" fill="currentColor"/>
          <circle cx="16" cy="14" r="1.5" fill="currentColor"/>
          <path d="M9 18c.83.67 1.83 1 3 1s2.17-.33 3-1"/>
        </svg>
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
  z-index: $z-chat-float;
}

.float-button {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  padding: 0;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  color: white;
  // 精致渐变：浅金→主金→深金
  background: linear-gradient(
    135deg,
    $color-accent-light 0%,
    $color-accent 50%,
    $color-accent-dark 100%
  );
  // 多层阴影，质感提升
  box-shadow:
    0 4px 16px rgba($color-accent, 0.25),
    0 8px 32px rgba($color-accent, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.15);
  transition: all $transition-base;
  overflow: visible;

  // 微妙脉冲光环
  &::before {
    content: '';
    position: absolute;
    inset: -4px;
    border-radius: inherit;
    border: 1px solid rgba($color-accent, 0.3);
    animation: gentle-pulse 2.5s ease-in-out infinite;
    pointer-events: none;
  }

  &:hover {
    transform: translateY(-3px);
    box-shadow:
      0 6px 20px rgba($color-accent, 0.35),
      0 12px 40px rgba($color-accent, 0.2),
      inset 0 1px 0 rgba(255, 255, 255, 0.2);

    // hover 时脉冲加速
    &::before {
      animation-duration: 1.5s;
    }

    .ai-icon {
      animation-play-state: paused;
    }
  }

  &:active {
    transform: translateY(-1px);
    box-shadow:
      0 2px 8px rgba($color-accent, 0.3),
      0 4px 16px rgba($color-accent, 0.15);
  }
}

// AI 图标微动效
.ai-icon {
  flex-shrink: 0;
  animation: icon-float 3s ease-in-out infinite;
}

// 脉冲动画
@keyframes gentle-pulse {
  0%, 100% {
    opacity: 0.5;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.02);
  }
}

// 图标悬浮
@keyframes icon-float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-2px);
  }
}

// 过渡动画
.float-fade-enter-active {
  animation: float-in 0.4s $transition-spring;
}

.float-fade-leave-active {
  animation: float-in 0.25s ease reverse;
}

@keyframes float-in {
  0% {
    opacity: 0;
    transform: scale(0.9) translateY(10px);
  }
  100% {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}
</style>
