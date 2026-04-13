<!--=====================================================
  CareerForge 弹窗头部组件
  @author Azir
=====================================================-->

<template>
  <div class="modal-header">
    <!-- 非默认状态才显示图标 -->
    <div
      v-if="isOptimizing || isCompleted || hasError"
      class="header-icon"
      :class="iconClass"
    >
      <!-- 优化中图标 -->
      <font-awesome-icon
        v-if="isOptimizing"
        icon="fa-solid fa-spinner"
        spin
      />
      <!-- 完成图标 -->
      <font-awesome-icon
        v-else-if="isCompleted"
        icon="fa-solid fa-circle-check"
      />
      <!-- 错误图标 -->
      <font-awesome-icon
        v-else-if="hasError"
        icon="fa-solid fa-circle-xmark"
      />
    </div>
    <h3 class="header-title">
      {{ title }}
    </h3>
    <button
      class="close-btn"
      @click="$emit('close')"
    >
      <font-awesome-icon icon="fa-solid fa-xmark" />
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
  font-size: 24px;

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
  font-size: 20px;
  color: $color-text-tertiary;
  border-radius: $radius-sm;
  transition: all $transition-fast;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: $color-text-primary;
  }
}
</style>
