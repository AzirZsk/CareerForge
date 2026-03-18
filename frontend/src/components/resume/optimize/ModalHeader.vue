<!--=====================================================
  LandIt 弹窗头部组件
  @author Azir
=====================================================-->

<template>
  <div class="modal-header">
    <div class="header-icon" :class="iconClass">
      <!-- 优化中图标 -->
      <svg v-if="isOptimizing" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M12 2v4m0 12v4M4.93 4.93l2.83 2.83m8.48 8.48l2.83 2.83M2 12h4m12 0h4M4.93 19.07l2.83-2.83m8.48-8.48l2.83-2.83">
          <animateTransform attributeName="transform" type="rotate" from="0 12 12" to="360 12 12" dur="2s" repeatCount="indefinite"/>
        </path>
      </svg>
      <!-- 完成图标 -->
      <svg v-else-if="isCompleted" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
        <polyline points="22 4 12 14.01 9 11.01"/>
      </svg>
      <!-- 错误图标 -->
      <svg v-else-if="hasError" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10"/>
        <line x1="15" y1="9" x2="9" y2="15"/>
        <line x1="9" y1="9" x2="15" y2="15"/>
      </svg>
    </div>
    <h3 class="header-title">{{ title }}</h3>
    <button class="close-btn" @click="$emit('close')">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <line x1="18" y1="6" x2="6" y2="18"/>
        <line x1="6" y1="6" x2="18" y2="18"/>
      </svg>
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  isOptimizing: boolean
  isCompleted: boolean
  hasError: boolean
  customTitle?: string
}>(), {
  customTitle: ''
})

defineEmits<{
  close: []
}>()

// 图标类
const iconClass = computed(() => ({
  'optimizing': props.isOptimizing,
  'completed': props.isCompleted,
  'error': props.hasError
}))

// 标题
const title = computed(() => {
  // 如果传入了自定义标题，直接使用
  if (props.customTitle) return props.customTitle
  if (props.isOptimizing) return 'AI 简历优化中...'
  if (props.isCompleted) return '优化完成'
  if (props.hasError) return '优化失败'
  return '简历优化'
})
</script>

<style lang="scss" scoped>
.modal-header {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  padding: $spacing-lg;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.header-icon {
  width: 40px;
  height: 40px;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;

  &.optimizing {
    background: rgba(212, 168, 83, 0.15);
    color: $color-accent;
  }

  &.completed {
    background: rgba(52, 211, 153, 0.15);
    color: $color-success;
  }

  &.error {
    background: rgba(248, 113, 113, 0.15);
    color: $color-error;
  }
}

.header-title {
  flex: 1;
  font-size: $text-lg;
  font-weight: $weight-semibold;
  color: $color-text-primary;
}

.close-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $color-text-tertiary;
  border-radius: $radius-sm;
  transition: all $transition-fast;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: $color-text-primary;
  }
}
</style>
