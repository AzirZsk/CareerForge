<!--=====================================================
  聊天窗口头部组件
  包含简历选择器和关闭按钮
  @author Azir
=====================================================-->

<template>
  <header class="chat-header">
    <div class="header-left">
      <!-- AI图标和标题 -->
      <div class="header-title">
        <AIIcon :size="28" />
        <span class="title">简历助手</span>
      </div>

      <!-- 简历选择器 -->
      <div class="resume-selector">
        <select
          :value="currentResumeId || ''"
          @change="handleResumeChange"
          class="resume-select"
        >
          <option value="">请选择简历</option>
          <option
            v-for="resume in resumeList"
            :key="resume.id"
            :value="resume.id"
          >
            {{ resume.name }}
          </option>
        </select>
        <svg class="select-arrow" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="6 9 12 15 18 9"></polyline>
        </svg>
      </div>
    </div>

    <!-- 关闭按钮 -->
    <button class="close-btn" @click="$emit('close')">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <line x1="18" y1="6" x2="6" y2="18"></line>
        <line x1="6" y1="6" x2="18" y2="18"></line>
      </svg>
    </button>
  </header>
</template>

<script setup lang="ts">
import type { ResumeOption } from '@/types/ai-chat'
import AIIcon from '@/components/common/AIIcon.vue'

interface Props {
  resumeList: ResumeOption[]
  currentResumeId: string | null
}

interface Emits {
  (e: 'resumeChange', resumeId: string | null): void
  (e: 'close'): void
}

defineProps<Props>()
const emit = defineEmits<Emits>()

function handleResumeChange(event: Event) {
  const target = event.target as HTMLSelectElement
  const resumeId = target.value || null
  emit('resumeChange', resumeId)
}
</script>

<style lang="scss" scoped>
.chat-header {
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

.resume-selector {
  position: relative;

  .resume-select {
    appearance: none;
    padding: $spacing-sm $spacing-xl $spacing-sm $spacing-md;
    background: $color-bg-secondary;
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: $radius-sm;
    color: $color-text-secondary;
    font-size: $text-sm;
    cursor: pointer;
    min-width: 180px;

    &:focus {
      outline: none;
      border-color: $color-accent;
    }

    option {
      background: $color-bg-secondary;
      color: $color-text-primary;
    }
  }

  .select-arrow {
    position: absolute;
    right: $spacing-sm;
    top: 50%;
    transform: translateY(-50%);
    pointer-events: none;
    color: $color-text-tertiary;
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
</style>
