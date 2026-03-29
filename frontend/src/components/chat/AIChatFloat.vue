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
        v-if="!state.isWindowOpen && !state.hideFloat"
        class="float-button"
        title="AI简历助手"
        @click="toggleWindow"
      >
        <!-- AI图标 -->
        <AIIcon
          class="ai-icon"
          :size="26"
        />
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
import AIIcon from '@/components/common/AIIcon.vue'

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
